<%@ page
	language="java"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
	trimDirectiveWhitespaces="true"
%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib  prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${current_locale}"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<title><spring:message code="header.userManagement"/></title>

<link
	rel="stylesheet"
	href="<c:url value='/css/bootstrap.min.css'/>"
/>
<link
	rel="stylesheet"
	type="text/css"
	href="<c:url value='/css/style.css' />"
/>
<link
	href="<c:url value='/css/font-awesome.min.css'/>"
	rel="stylesheet"
/>
<script
	type="text/javascript"
	src="<c:url value='/js/jquery-2.1.3.min.js'/>"
></script>

<script	src="<c:url value='/js/lib/popper.min.js'/>" ></script>
<script	src="<c:url value='/js/lib/bootstrap.min.js'/>"></script>
<script
	type="text/javascript"
	src="<c:url value='/js/arc.js'/>"
></script>
<script
	type="text/javascript"
	src="<c:url value='/js/gererFamilleNorme.js'/>"
></script>
<script
	type="text/javascript"
	src="<c:url value='/js/component.js'/>"
></script>
</head>
<body class="bg-light">
<form id="selectGererUtilisateurs"
	action="selectGererUtilisateurs.action"
	spellcheck="false"
	method="post"
>

	<%@include file="tiles/header.jsp"%>

	<div class="container-fluid">
			<div class="row">
				<div
					class="col-md-4"
					class="aside"
				>
					<div class="row">
						<!-- affichage de la liste des utilisateurs -->
						<c:set var="view" value="${viewListProfils}"  scope="request"/>
						<c:import url="tiles/templateVObject.jsp">
							<c:param name="taille" value ="col-md" />
							<c:param name="ligneAdd" value="true" />
							<c:param name="btnSelect" value="true" />
							<c:param name="btnSee" value="true" />
							<c:param name="btnSort" value="true" />
							<c:param name="btnAdd" value="true" />
							<c:param name="btnUpdate" value="true" />
							<c:param name="btnDelete" value="true" />
							<c:param name="checkbox" value="true" />
							<c:param name="checkboxVisible" value="true" />
							<c:param name="extraScopeSee" value="viewListUtilisateursDuProfil;" />
						</c:import>
					</div>

					<div class="row">
						<!-- VIEW TABLE UTILISATEURS -->
						<c:set var="view" value="${viewListUtilisateursDuProfil}"  scope="request"/>
						<c:import url="tiles/templateVObject.jsp">
							<c:param name="taille" value ="col-md" />
							<c:param name="ligneAdd" value="true" />
							<c:param name="btnSelect" value ="true" />
							<c:param name="btnSee" value ="true" />
							<c:param name="btnSort" value ="true" />
							<c:param name="btnAdd" value ="true" />
							<c:param name="btnUpdate" value ="true" />
							<c:param name="btnDelete" value ="true" />
							<c:param name="checkbox" value ="true" />
							<c:param name="checkboxVisible" value ="true" />
						</c:import>
					</div>
				</div>

		</form>

	</div>


</body>
</html>