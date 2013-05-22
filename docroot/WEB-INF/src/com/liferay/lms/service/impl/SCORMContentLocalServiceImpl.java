/**
 * 2013 TELEFONICA LEARNING SERVICES. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.lms.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;

import com.liferay.lms.model.Course;
import com.liferay.lms.model.LmsPrefs;
import com.liferay.lms.model.SCORMContent;
import com.liferay.lms.service.base.SCORMContentLocalServiceBaseImpl;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.model.LayoutSetPrototype;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetPrototypeServiceUtil;
import com.liferay.portal.service.ServiceContext;

/**
 * The implementation of the s c o r m content local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.liferay.lms.service.SCORMContentLocalService} interface.
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author TLS
 * @see com.liferay.lms.service.base.SCORMContentLocalServiceBaseImpl
 * @see com.liferay.lms.service.SCORMContentLocalServiceUtil
 */
public class SCORMContentLocalServiceImpl
	extends SCORMContentLocalServiceBaseImpl {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never reference this interface directly. Always use {@link com.liferay.lms.service.SCORMContentLocalServiceUtil} to access the s c o r m content local service.
	 */
	public String getBaseDir()
	{
		String baseDir=PropsUtil.get("liferay.lms.scormdir");
		if(baseDir==null ||baseDir.equals(""))
		{
			baseDir=PropsUtil.get("liferay.home")+"/data/scorms";
		}
		return baseDir;
	}
	public String getDirScormPath(SCORMContent scocon)
	{
		String baseDir=PropsUtil.get("liferay.lms.scormdir");
		if(baseDir==null ||baseDir.equals(""))
		{
			baseDir=PropsUtil.get("liferay.home")+"/data/scorms";
		}
		return baseDir+"/"+Long.toString(scocon.getCompanyId())+"/"+Long.toString(scocon.getGroupId())+"/"+scocon.getUuid();
	}
	public java.util.List<SCORMContent> getSCORMContentOfGroup(long groupId) throws SystemException
	{
		return scormContentPersistence.findByGroupId(groupId);
	}
	public java.util.List<SCORMContent> getSCORMContents(long companyId) throws SystemException
	{
		return scormContentPersistence.findByCompanyId(companyId);
	}
	public long countByGroupId(long groupId) throws SystemException
	{
		return scormContentPersistence.countByGroupId(groupId);
	}
	public void delete (long scormId) throws SystemException, PortalException
	{
		SCORMContent scorm=scormContentPersistence.fetchByPrimaryKey(scormId);
		assetEntryLocalService.deleteEntry(SCORMContent.class.getName(),scormId);
		resourceLocalService.deleteResource(
		scorm.getCompanyId(), SCORMContent.class.getName(),
		ResourceConstants.SCOPE_INDIVIDUAL, scormId);
		scormContentPersistence.remove(scorm);
	}
	public SCORMContent updateSCORMContent(SCORMContent scocontent,ServiceContext serviceContext) throws PortalException, SystemException
	{
		scocontent.setExpandoBridgeAttributes(serviceContext);
		scormContentPersistence.update(scocontent, true);
		assetEntryLocalService.updateEntry(
				scocontent.getUserId(), scocontent.getGroupId(), SCORMContent.class.getName(),
				scocontent.getScormId(), scocontent.getUuid(),0, serviceContext.getAssetCategoryIds(),
				serviceContext.getAssetTagNames(), true, null, null,
				new java.util.Date(System.currentTimeMillis()), null,
				ContentTypes.TEXT_HTML, scocontent.getTitle(),null,  scocontent.getDescription(),null, null, 0, 0,
				null, false);
		return scocontent;
	}
	public SCORMContent addSCORMContent (String title, String description,File scormfile,
		ServiceContext serviceContext)
			throws SystemException, 
			PortalException, IOException {
		
		SCORMContent scocontent =
				scormContentPersistence.create(counterLocalService.increment(
						SCORMContent.class.getName()));
			long userId=serviceContext.getUserId();
			scocontent.setCompanyId(serviceContext.getCompanyId());
			scocontent.setGroupId(serviceContext.getScopeGroupId());
			scocontent.setUserId(userId);
			scocontent.setDescription(description);
			scocontent.setTitle(title);
			scocontent.setStatus(WorkflowConstants.STATUS_APPROVED);
			scocontent.setExpandoBridgeAttributes(serviceContext);
			scormContentPersistence.update(scocontent, true);
			String dirScorm=getDirScormPath(scocontent);
			File dir=new File(dirScorm);
			FileUtils.forceMkdir(dir);
			try {
				ZipFile zipFile= new ZipFile(scormfile);
				zipFile.extractAll(dir.getCanonicalPath());
				File manifestfile=new File(dir.getCanonicalPath()+"/imsmanifest.xml");
				try {
					Document imsdocument=SAXReaderUtil.read(manifestfile);
					Element item=imsdocument.getRootElement().element("organizations").elements("organization").get(0).elements("item").get(0);
					String resourceid=item.attributeValue("identifierref");
					java.util.List<Element> resources=imsdocument.getRootElement().element("resources").elements("resource");
					for(Element resource:resources)
					{
						if(resource.attributeValue("identifier").equals(resourceid))
						{
							scocontent.setIndex(resource.attributeValue("href"));
							scormContentPersistence.update(scocontent, true);
						}
					}
				} catch (DocumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (ZipException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		
				if (serviceContext.isAddGroupPermissions() ||
						serviceContext.isAddGuestPermissions()) {
					resourceLocalService.addResources(
							serviceContext.getCompanyId(), serviceContext.getScopeGroupId(), userId,
							SCORMContent.class.getName(), scocontent.getPrimaryKey(), false,
						serviceContext.isAddGroupPermissions(),
						serviceContext.isAddGuestPermissions());			
					}
					else {
						
						resourceLocalService.addModelResources(
								serviceContext.getCompanyId(), serviceContext.getScopeGroupId(), userId,
								SCORMContent.class.getName(), scocontent.getPrimaryKey(),
						serviceContext.getGroupPermissions(),
						serviceContext.getGuestPermissions());
						
						
					}
			
			assetEntryLocalService.updateEntry(
					userId, scocontent.getGroupId(), SCORMContent.class.getName(),
					scocontent.getScormId(), scocontent.getUuid(),0, serviceContext.getAssetCategoryIds(),
					serviceContext.getAssetTagNames(), true, null, null,
					new java.util.Date(System.currentTimeMillis()), null,
					ContentTypes.TEXT_HTML, scocontent.getTitle(),null,  scocontent.getDescription(),null, null, 0, 0,
					null, false);
					
			
				
			return scocontent;
		
	}
}