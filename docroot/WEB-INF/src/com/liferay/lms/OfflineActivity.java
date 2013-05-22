package com.liferay.lms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.liferay.lms.model.LearningActivity;
import com.liferay.lms.model.LearningActivityResult;
import com.liferay.lms.model.LearningActivityTry;
import com.liferay.lms.service.LearningActivityLocalServiceUtil;
import com.liferay.lms.service.LearningActivityResultLocalServiceUtil;
import com.liferay.lms.service.LearningActivityTryLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.NestableException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;


/**
 * Portlet implementation class OfflineActivity
 */
public class OfflineActivity extends MVCPortlet {
	
	public static final String ACTIVITY_RESULT_PASSED_SQL = "WHERE (EXISTS (SELECT 1 FROM lms_learningactivityresult " +
			"WHERE User_.userId = lms_learningactivityresult.userId" +
			" AND lms_learningactivityresult.passed > 0 AND lms_learningactivityresult.actId = ? ))"; 
	
	public static final String ACTIVITY_RESULT_FAIL_SQL = "WHERE (EXISTS (SELECT 1 FROM lms_learningactivityresult " +
			"WHERE User_.userId = lms_learningactivityresult.userId" +
			" AND lms_learningactivityresult.passed = 0 AND lms_learningactivityresult.actId = ? ))"; 
	
	public static final String ACTIVITY_RESULT_NO_CALIFICATION_SQL = "WHERE (NOT EXISTS (SELECT 1 FROM lms_learningactivityresult " +
			"WHERE User_.userId = lms_learningactivityresult.userId AND lms_learningactivityresult.actId = ? ))"; 
	
	@Override
	public void serveResource(ResourceRequest resourceRequest,
			ResourceResponse resourceResponse) throws IOException,
			PortletException {
		String action = ParamUtil.getString(resourceRequest, "action");
		long actId = ParamUtil.getLong(resourceRequest, "actId",0);
		if(action.equals("export")){
			
			try {
				//Necesario para crear el fichero csv.
				resourceResponse.setCharacterEncoding("ISO-8859-1");
				resourceResponse.setContentType("text/csv;charset=ISO-8859-1");
				resourceResponse.addProperty(HttpHeaders.CONTENT_DISPOSITION,"attachment; fileName=data.csv");
		        byte b[] = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
		        
		        resourceResponse.getPortletOutputStream().write(b);
		        
		        CSVWriter writer = new CSVWriter(new OutputStreamWriter(resourceResponse.getPortletOutputStream(),"ISO-8859-1"),';');
		        String[] cabeceras = new String[4];
		        
		        
		        //En esta columna vamos a tener el nombre del usuario.
		        cabeceras[0]="User";
		        cabeceras[1]="Date";
		        cabeceras[2]="Result";
		        cabeceras[3]="Comment";
		        		    
		        writer.writeNext(cabeceras);
		        DynamicQuery dq=DynamicQueryFactoryUtil.forClass(LearningActivityResult.class);
		      	Criterion criterion=PropertyFactoryUtil.forName("actId").eq(actId);
				dq.add(criterion);
				
				//Partiremos del usuario para crear el csv para que sea más facil ver los intentos.
		        List<LearningActivityResult> listresult = LearningActivityResultLocalServiceUtil.dynamicQuery(dq);
		        for(LearningActivityResult learningActivityResult:listresult){
		        			//Array con los resultados de los intentos.
			        		String[] resultados = new String[4];
			        		//En la primera columna del csv introducidos el nombre del estudiante.
			        		resultados[0] = String.valueOf(learningActivityResult.getUuid());
			        		resultados[1] = String.valueOf(learningActivityResult.getEndDate());
			        		resultados[2] = String.valueOf(learningActivityResult.getResult());
			        		resultados[3] = String.valueOf(learningActivityResult.getComments());
			        		
			        		//Escribimos las respuestas obtenidas para el intento en el csv.
			    			writer.writeNext(resultados);
		        }
		        writer.flush();
				writer.close();
				resourceResponse.getPortletOutputStream().flush();
				resourceResponse.getPortletOutputStream().close();
			
			} catch (SystemException e) {

			}finally{
				resourceResponse.getPortletOutputStream().flush();
				resourceResponse.getPortletOutputStream().close();
			}
		} 
	}
		
	private void importGrades(RenderRequest renderRequest,
			RenderResponse renderResponse) throws PortletException, IOException {
		UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(renderRequest);
		
		List<String> errors= new ArrayList<String>();
		renderRequest.setAttribute("errorsInCSV", errors);
		
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
		Locale locale = themeDisplay.getLocale();
		
		long actId = ParamUtil.getLong(renderRequest,"actId");
		long groupId = themeDisplay.getScopeGroupId();
		File csvFile = uploadRequest.getFile("fileName");
		
		if(csvFile==null){
			errors.add(LanguageUtil.get(getPortletConfig(),locale,"offlinetaskactivity.csvError.empty-file"));
		}else{
			CSVReader reader=null;
			try {
				reader = new CSVReader(new FileReader(csvFile));
				int line=0;
				String[] currLine;
				while ((currLine = reader.readNext()) != null) {
					boolean correct=true;
					line++;
					
					if(currLine.length==3){
						
						long userId=0;
						String userFullName=StringPool.BLANK;
						long result=0;
						
						try {
							userId=Long.parseLong(currLine[0].trim());
						} catch (NumberFormatException e) {
							correct=false;
							errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.user-id-bad-format",new Object[]{line},false));
						}
						
						if(correct) {
							try {
								User user=UserLocalServiceUtil.getUser(userId);
								userFullName=user.getFullName();
								if(!ArrayUtil.contains(user.getGroupIds(),groupId)){
									correct=false;
									errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.user-id-not-in-course",new Object[]{line},false));
								}
							} catch (PortalException e) {
								correct=false;
								errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.user-id-not-exists",new Object[]{line},false));
							} catch (SystemException e) {
								correct=false;
								errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.user-id-system-error",new Object[]{line},false));
							}
						}	
	
						try {
							result=Long.parseLong(currLine[1]);
							if(result<0 || result>100){
								correct=false;
								errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.result-bad-format",new Object[]{line},false));
							}
						} catch (NumberFormatException e) {
							correct=false;
							errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.result-bad-format",new Object[]{line},false));
						}
											
						if(correct){
							try {
								LearningActivityTry  learningActivityTry =  LearningActivityTryLocalServiceUtil.getLastLearningActivityTryByActivityAndUser(actId, userId);
								if(learningActivityTry==null){
									ServiceContext serviceContext = new ServiceContext();
									serviceContext.setUserId(userId);
									learningActivityTry =  LearningActivityTryLocalServiceUtil.createLearningActivityTry(actId,serviceContext);
								}
								learningActivityTry.setEndDate(new Date());
								learningActivityTry.setResult(result);
								learningActivityTry.setComments(currLine[2]);
								updateLearningActivityTryAndResult(learningActivityTry);
	
							} catch (NestableException e) {
								correct=false;
								errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.bad-updating",new Object[]{line,userFullName},false));
							}
						}	
					}
					else {
						if(currLine.length!=0) {
							correct=false;
							errors.add(LanguageUtil.format(getPortletConfig(),locale,"offlinetaskactivity.csvError.bad-format",new Object[]{line},false));
						}
					}
				}
	
			} catch(FileNotFoundException e) {
				errors.add(LanguageUtil.get(getPortletConfig(),locale,"offlinetaskactivity.csvError.empty-file"));
			} finally {
				if(reader!=null) {
					reader.close();
				}
			}
		}
	}
	
	private void setGrades(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		
		boolean correct=true;
		long actId = ParamUtil.getLong(renderRequest,"actId"); 
		long studentId = ParamUtil.getLong(renderRequest,"studentId");
		String comments = renderRequest.getParameter("comments");

		long result=0;
		try {
			result=Long.parseLong(renderRequest.getParameter("result"));
			if(result<0 || result>100){
				correct=false;
				SessionErrors.add(renderRequest, "offlinetaskactivity.grades.result-bad-format");
			}
		} catch (NumberFormatException e) {
			correct=false;
			SessionErrors.add(renderRequest, "offlinetaskactivity.grades.result-bad-format");
		}
		
		if(correct) {
			try {
				LearningActivityTry  learningActivityTry =  LearningActivityTryLocalServiceUtil.getLastLearningActivityTryByActivityAndUser(actId, studentId);
				if(learningActivityTry==null){
					ServiceContext serviceContext = new ServiceContext();
					serviceContext.setUserId(studentId);
					learningActivityTry =  LearningActivityTryLocalServiceUtil.createLearningActivityTry(actId,serviceContext);
				}
				learningActivityTry.setEndDate(new Date());
				learningActivityTry.setResult(result);
				learningActivityTry.setComments(comments);
				updateLearningActivityTryAndResult(learningActivityTry);
				
				SessionMessages.add(renderRequest, "offlinetaskactivity.grades.updating");
			} catch (NestableException e) {
				SessionErrors.add(renderRequest, "offlinetaskactivity.grades.bad-updating");
			}
		}
	}

	private void updateLearningActivityTryAndResult(
			LearningActivityTry learningActivityTry) throws PortalException,
			SystemException {
		LearningActivityTryLocalServiceUtil.updateLearningActivityTry(learningActivityTry);
		
		LearningActivityResult learningActivityResult = LearningActivityResultLocalServiceUtil.getByActIdAndUserId(learningActivityTry.getActId(), learningActivityTry.getUserId());
		if(learningActivityResult.getResult() != learningActivityTry.getResult()) {
			LearningActivity learningActivity = LearningActivityLocalServiceUtil.getLearningActivity(learningActivityTry.getActId());
			learningActivityResult.setResult(learningActivityTry.getResult());
			learningActivityResult.setPassed(learningActivityTry.getResult()>=learningActivity.getPasspuntuation());
			LearningActivityResultLocalServiceUtil.updateLearningActivityResult(learningActivityResult);
		}
	}
	
	@Override
	protected void doDispatch(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		String ajaxAction = renderRequest.getParameter("ajaxAction");
		
		if(ajaxAction!=null) {
			if("importGrades".equals(ajaxAction)) {
				importGrades(renderRequest, renderResponse);
			}
			else if("setGrades".equals(ajaxAction)) {
				setGrades(renderRequest, renderResponse);
			} 
		}
		
		
		super.doDispatch(renderRequest, renderResponse);
	}
	
	public void edit(ActionRequest actionRequest,ActionResponse actionResponse)throws Exception {

		actionResponse.setRenderParameters(actionRequest.getParameterMap());
		if(ParamUtil.getLong(actionRequest, "actId", 0)==0)
		{
			actionResponse.setRenderParameter("jspPage", "/html/offlinetaskactivity/admin/edit.jsp");
		}
	}
	
			
}
