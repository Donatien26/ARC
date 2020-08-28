
<!-- Voir le guide dans siera/siera-webutils/documentation -->

<!-- Pour appeler cette jsp, il faut copier les lignes suivantes: -->
<%-- <s:include value="templateVObject.jsp"> --%>
<%-- 		<s:set var="view" value="%{viewProfil}" scope="request"></s:set> --%>
<%-- 		<s:param name="taille" >col-md-4</s:param> --%>
<%-- 		<s:param name="btnSelect" >true</s:param> --%>
<%-- 		<s:param name="btnSee" >true</s:param> --%>
<%-- 		<s:param name="btnSort" >true</s:param> --%>
<%-- 		<s:param name="btnAdd" >true</s:param> --%>
<%-- 		<s:param name="btnUpdate" >true</s:param> --%>
<%-- 		<s:param name="btnDelete" >true</s:param> --%>
<%-- 		<s:param name="ligneAdd" >true</s:param> --%>
<%-- 		<s:param name="ligneFilter" >true</s:param> --%>
<%-- 		<s:param name="checkbox" >true</s:param> --%>
<%-- 		<s:param name="checkboxVisible" >true</s:param> --%>
<%-- 		<s:param name="multiSelection" >true</s:param> --%>
<%-- 		<s:param name="extraScopeAdd" >aScope</s:param> --%>
<%-- 		<s:param name="extraScopeDelete" >aScope</s:param> --%>
<%-- 		<s:param name="extraScopeUpdate" >aScope</s:param> --%>
<%-- 		<s:param name="extraScopeSee" >aScope</s:param> --%>
<%-- 		<s:param name="otherButton">'[button1, button2 ...]</s:param> --%>
<%-- 	</s:include> --%>

<%@ taglib
	prefix="s"
	uri="/struts-tags"
%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@taglib  prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${current_locale}"/>

<s:set
	var="view"
	value="#request.view"
/>
<s:set
	var="viewType"
	value="#view.sessionName.substring('view'.length)"
></s:set>
<s:set var="taille">${param.taille}</s:set>
<s:set var="btnSelect">${param.btnSelect}</s:set>
<s:set var="btnSee">${param.btnSee}</s:set>
<s:set var="btnSort">${param.btnSort}</s:set>
<s:set var="btnAdd">${param.btnAdd}</s:set>
<s:set var="btnUpdate">${param.btnUpdate}</s:set>
<s:set var="btnDelete">${param.btnDelete}</s:set>
<s:set var="ligneAdd">${param.ligneAdd}</s:set>
<s:set var="ligneFilter">${param.ligneFilter}</s:set>
<s:set var="checkbox">${param.checkbox}</s:set>
<s:set var="checkboxVisible">${param.checkboxVisible}</s:set>
<s:set var="multiSelection">${param.multiSelection}</s:set>
<s:set var="extraScopeSelect">${param.extraScopeSelect}</s:set>
<s:set var="extraScopeUpdate">${param.extraScopeUpdate}</s:set>
<s:set var="extraScopeDelete">${param.extraScopeDelete}</s:set>
<s:set var="extraScopeAdd">${param.extraScopeAdd}</s:set>
<s:set var="extraScopeSee">${param.extraScopeSee}</s:set>
<s:set var="otherButton">${param.otherButton}</s:set>
<s:set var="allowResize">${param.allowResize}</s:set>

<div
	id="<s:property value="#view.sessionName"/>"
	class='<s:property value="%{#taille}"/>'
>
	<s:if test="%{#view.isInitialized==true&&#view.isScoped==true}">
		<div class="row">
			<div class="col-md">
				<div class="card  no-margin">
					<div class="card-header bg-primary p-0">
						<h3 class="text-white m-1"><spring:message code="${view.title}"/></h3>
					</div>
					<div class="card-body p-0">
						<s:hidden
							name="%{#view.sessionName}.headerSortDLabel"
							value=""
						/>
						<table class="fixedHeader w-100 ">
							<thead>
								<tr>
									<s:if test="#checkbox.equals('true')">
										<s:if test="#checkboxVisible.equals('false')">
											<th style="display: none;"></th>
										</s:if>
										<s:else>
											<th></th>
										</s:else>
									</s:if>
									<s:iterator
										value="%{#view.headersVLabel}"
										var="head"
										status="incr"
									>
										<s:if test="%{#view.headersVisible[#incr.index]}">
											<th
												class="sort"
												scope="col"
												style="width:<s:property value='%{#view.headersVSize[#incr.index]}'/>;"
											><spring:message code="${view.headersVLabel[incr.index]}"/>
											</th>
										</s:if>
										<s:else>
											<th style="display: none;"><s:property /></th>
										</s:else>
									</s:iterator>
							</tr>
								<tr style="display: none;">
									<th></th>
									<s:iterator
										value="%{#view.headersDLabel}"
										var="head"
										status="incr"
									>
										<th><s:property /></th>
									</s:iterator>
							</tr>
								<tr style="display: none;">
									<s:if test="#checkbox.equals('true')">
										<s:if test="#checkboxVisible.equals('false')">
											<th style="display: none;"></th>
										</s:if>
										<s:else>
											<th></th>
										</s:else>
									</s:if>
									<s:iterator
										value="%{#view.headersDLabel}"
										var="head"
										status="incr"
									>
										<th>
											<s:checkbox
												name="%{#view.sessionName}.selectedColumns[%{#incr.index}]"
												theme="simple">
												</s:checkbox>
										</th>
									</s:iterator>
							</tr>
								<s:if test="#ligneFilter.equals('true')">
									<tr>
										<s:if test="#checkbox.equals('true')">
											<s:if test="#checkboxVisible.equals('false')">
												<th style="display: none;"></th>
											</s:if>
											<s:else>
												<th style="text-align:center; font-weight:bold;font-size:1.5em;"></th>
											</s:else>
										</s:if>
										<s:iterator
											value="%{#view.headersVLabel}"
											var="head"
											status="incr"
										>
											<s:if test="%{#view.headersVisible[#incr.index]}">
												<th><s:textarea
														name="%{#view.sessionName}.filterFields[%{#incr.index}]"
														value="%{#view.filterFields[#incr.index]}"
														theme="simple"
													></s:textarea></th>
											</s:if>
											<s:else>
												<th style="display: none;"><s:textarea
														name="%{#view.sessionName}.filterFields[%{#incr.index}]"
														value="%{#view.filterFields[#incr.index]}"
														theme="simple"
													></s:textarea></th>
											</s:else>
										</s:iterator>
									</tr>
								</s:if>
						</thead>
							<tbody>
								<s:if test="%{#view.content!=null && #view.content.size()>0}">
									<s:iterator
										value="#view.content"
										var="line"
										status="incr1"
									>
										<tr>
											<s:if test="#checkbox.equals('true')">
												<s:if test="#checkboxVisible.equals('false')">
													<td style="display: none">
												</s:if>
												<s:else>
													<td>
												</s:else>
												<s:if test="#multiSelection.equals('true')">
													<s:checkbox
														class="chooseLine"
														name="%{#view.sessionName}.selectedLines[%{#incr1.index}]"
														theme="simple"
													></s:checkbox>
												</s:if>
												<s:else>
													<s:checkbox
														class="chooseLine"
														name="%{#view.sessionName}.selectedLines[%{#incr1.index}]"
														onclick="updateCheckBox('%{#view.sessionName}',$(this));"
														theme="simple"
													></s:checkbox>
												</s:else>
												</td>
											</s:if>
											<s:iterator
												value="#line"
												status="incr2"
											>
												<s:if test="%{#view.headersVisible[#incr2.index]}">
													<td><s:if
															test='%{#view.headersUpdatable[#incr2.index]}'
														>
															<s:if
																test='%{"text".equals(#view.headersVType[#incr2.index])}'
															>
																<s:textarea
																	name="%{#view.sessionName}.content.t[%{#incr1.index}].d[%{#incr2.index}]"
																	value="%{#view.content.t[#incr1.index].d[#incr2.index]}"
																	theme="simple"
																></s:textarea>
															</s:if>

															<s:elseif
																test='%{"datepicker".equals(#view.headersVType[#incr2.index])}'
															>
																<s:textfield
																	class="datepicker full-width"
																	type2="date"
																	dateFormat="yyyy-mm-dd"
																	name="%{#view.sessionName}.content.t[%{#incr1.index}].d[%{#incr2.index}]"
																	value="%{#view.content.t[#incr1.index].d[#incr2.index]}"
																	theme="simple"
																></s:textfield>
															</s:elseif>
															<s:elseif
																test='%{"multiSelect".equals(#view.headersVType[#incr2.index])}'
															>
																<s:textarea
																	name="%{#view.sessionName}.content.t[%{#incr1.index}].d[%{#incr2.index}]"
																	value="%{#view.content.t[#incr1.index].d[#incr2.index]}"
																	theme="simple"
																></s:textarea>
															</s:elseif>
															<s:else>
																<s:select
																class="w-100"
																	list="%{#view.headersVSelect[#incr2.index]}"
																	name="%{#view.sessionName}.content.t[%{#incr1.index}].d[%{#incr2.index}]"
																	value="%{#view.content.t[#incr1.index].d[#incr2.index]}"
																	theme="simple"
																></s:select>
															</s:else>
														</s:if> <s:else>
															<s:textarea
																name="%{#view.sessionName}.content.t[%{#incr1.index}].d[%{#incr2.index}]"
																value="%{#view.content.t[#incr1.index].d[#incr2.index]}"
																theme="simple"
																readonly="true"
															></s:textarea>
														</s:else></td>
												</s:if>
												<s:else>
													<td style="display: none;"><s:if
															test='%{"text".equals(#view.headersVType[#incr2.index])}'
														>
															<s:textarea
																name="%{#view.sessionName}.content.t[%{#incr1.index}].d[%{#incr2.index}]"
																value="%{#view.content.t[#incr1.index].d[#incr2.index]}"
																theme="simple"
															></s:textarea>
														</s:if> <s:else>
															<s:select
															class="w-100"
																list="%{#view.headersVSelect[#incr2.index]}"
																name="%{#view.sessionName}.content.t[%{#incr1.index}].d[%{#incr2.index}]"
																value="%{#view.content.t[#incr1.index].d[#incr2.index]}"
																theme="simple"
															></s:select>
														</s:else></td>
												</s:else>
											</s:iterator>
										</tr>
									</s:iterator>
								</s:if>
								<s:else>
									<s:if test="hasActionErrors()">
										<s:actionerror />
									</s:if>
									<s:else>
									</s:else>
								</s:else>
								<s:if test="#ligneAdd.equals('true')">
									<tr style="background-color: #fffaee;">
										<s:if test="#checkbox.equals('true')">
											<td style="text-align:center; font-weight:bold;font-size:1.5em;">+</td>
										</s:if>
										<s:iterator
											value="%{#view.headersDLabel}"
											var="input"
											status="incr"
										>
											<s:if test="%{#view.headersVisible[#incr.index]}">
												<s:if test="%{!#view.headersUpdatable[#incr.index]}">
													<td><s:textarea
															name="#view.inputFields[%{#incr.index}]"
															theme="simple"
															readonly="true"
														></s:textarea></td>
												</s:if>
												<s:else>
													<td><s:if
															test='%{"text".equals(#view.headersVType[#incr.index])}'
														>
															<s:textarea
																name="%{#view.sessionName}.inputFields[%{#incr.index}]"
																theme="simple"
															></s:textarea>
														</s:if> <s:elseif
															test='%{"datepicker".equals(#view.headersVType[#incr.index])}'
														>
															<s:textfield
																class="datepicker full-width"
																type2="date"
																dateFormat="yyyy-mm-dd"
																name="%{#view.sessionName}.inputFields[%{#incr.index}]"
																theme="simple"
															></s:textfield>
														</s:elseif> <s:elseif
															test='%{"multiSelect".equals(#view.headersVType[#incr.index])}'
														>
															<s:select
															class="w-100"
																multiple="true"
																list="%{#view.headersVSelect[#incr.index]}"
																emptyOption="true"
																name="%{#view.sessionName}.inputFields[%{#incr.index}]"
																theme="simple"
															></s:select>
														</s:elseif> <s:else>
															<s:select
															class="w-100"
																emptyOption="true"
																list="%{#view.headersVSelect[#incr.index]}"
																name="%{#view.sessionName}.inputFields[%{#incr.index}]"
																theme="simple"
															></s:select>
														</s:else></td>
												</s:else>
											</s:if>
											<s:else>
												<td style="display: none;"><s:if
														test='%{"text".equals(#view.headersVType[#incr.index])}'
													>
														<s:textarea
															name="%{#view.sessionName}.inputFields[%{#incr.index}]"
															theme="simple"
														></s:textarea>
													</s:if> <s:else>
														<s:select
														class="w-100"
															requiredLabel="true"
															list="%{#view.headersVSelect[#incr.index]}"
															value="%{#view.headersVSelect[#incr.index][0]}"
															name="%{#view.sessionName}.inputFields[%{#incr.index}]"
															theme="simple"
														></s:select>
													</s:else></td>
											</s:else>
										</s:iterator>
									</tr>
								</s:if>
						</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>
		<div class="alert">
			<s:property value="%{#view.message}" />
		</div>
		<!--            Les boutons pour faire d�filer les pages de r�sultat -->

		<div class="row mt-3">
			<!--             Les boutons d'action -->

			<div class='col-md'>
				<div
					class="btn-text-sm"
					role="group"
					style="float: left;"
				>
					<s:if test="#btnSelect.equals('true')">
						<button
							id="<s:property value="#view.sessionName"/>.select"
							class="btn btn-secondary btn-sm "
							type="submit"
							doAction="select<s:property value="#viewType"/>"
							scope="<s:property value="#view.sessionName"/>;"
							value="<spring:message code="gui.button.refresh"/>"
						><span class="fa fa-refresh">&nbsp;</span><spring:message code="gui.button.refresh"/></button>
					</s:if>
					<s:if test="#btnSee.equals('true')">
						<button
							id="<s:property value="#view.sessionName"/>.see"
							style="display: none;"
							class="btn btn-secondary btn-sm"
							type="submit"
							doAction="select<s:property value="#viewType"/>"
							scope="${param.extraScopeSee}"
							value="<spring:message code="gui.button.see"/>"
						><span class="fa fa-eye-open">&nbsp;</span><spring:message code="gui.button.see"/></button>
					</s:if>
					<s:if test="#btnSort.equals('true')">
						<button
							id="<s:property value="#view.sessionName"/>.sort"
							style="display: none;"
							class="btn btn-secondary btn-sm""
							type="submit"
							doAction="sort<s:property value="#viewType"/>"
							scope="<s:property value="#view.sessionName"/>;"
							value="<spring:message code="gui.button.sort"/>"
						><span class="fa fa-sort">&nbsp;</span><spring:message code="gui.button.sort"/></button>
					</s:if>
					<s:if test="#btnAdd.equals('true')">
						<button
							id="<s:property value="#view.sessionName"/>.add"
							class="btn btn-primary btn-sm "
							type="submit"
							doAction="add<s:property value="#viewType"/>"
							scope="<s:property value="#view.sessionName"/>;${param.extraScopeAdd}"
							value="<spring:message code="gui.button.add"/>"
						><span class="fa fa-check">&nbsp;</span><spring:message code="gui.button.add"/></button>
					</s:if>
					<s:if test="#btnUpdate.equals('true')">
						<button
							id="<s:property value="#view.sessionName"/>.update"
							class="btn btn-primary btn-sm "
							type="submit"
							doAction="update<s:property value="#viewType"/>"
							scope="<s:property value="#view.sessionName"/>;${param.extraScopeUpdate}"
							value="<spring:message code="gui.button.update"/>"
						><span class="fa fa-save">&nbsp;</span><spring:message code="gui.button.update"/></button>
					</s:if>
					<s:if test="#btnDelete.equals('true')">
						<button
							id="<s:property value="#view.sessionName"/>.delete"
							class="btn btn-primary btn-sm "
							type="submit"
							doAction="delete<s:property value="#viewType"/>"
							scope="<s:property value="#view.sessionName"/>;${param.extraScopeDelete}"
							value="<spring:message code="gui.button.delete"/>"
						><span class="fa fa-remove">&nbsp;</span><spring:message code="gui.button.delete"/></button>
					</s:if>

					<s:if test="#otherButton != null">
						<s:iterator
							value="%{#otherButton.split('\\\|')}"
							var="button"
						>
							<s:property
								value="%{button}"
								escapeHtml="false"
							/>
						</s:iterator>
					</s:if>
				</div>
				
				<s:include value="template_page_manager.jsp">
					<s:set var="view" value="%{view}" scope="request"></s:set>
					<s:param name="allowResize"><s:property value="allowResize"/></s:param>
				</s:include>
			</div>
		</div>
		<br>
	</s:if>
</div>