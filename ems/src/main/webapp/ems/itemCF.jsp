<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page  contentType="text/html; UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<title>emplist</title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/ems/css/style.css" />
	</head>
	<body>
		<div id="wrap">
			<div id="top_left">
				<div id="header">
					<div id="rightheader">
						<p>
							2020/4/12
							<br />
						</p>
					</div>
					<div id="topheader">
						<h1 id="title">
							<a href="#">XiaoHu音乐推荐系统 1.0</a>
						</h1>
					<p>
                        <input type="button" class="button" value="退出登陆" onclick="location='${pageContext.request.contextPath}/ems/login.jsp'"/>
                    </p>
                    <p>
                        <input type="button" class="button" value="返回首页" onclick="location='${pageContext.request.contextPath}/user/login?username=${sessionScope.user.username}&password=${sessionScope.user.password}'"/>
                    </p>
					</div>
					<div id="navigation">
					</div>
				</div>
				<div id="content">
					<p id="whereami">
					</p>
					<h1>
						Welcome ${sessionScope.user.username}! 根据当前音乐基于协同过滤算法的推荐如下：
						${requestScope.itemNull}
					</h1>
					<table class="table">
						<tr class="table_header">
							<td>
								歌曲名称
							</td>
							<td>

							</td>
							<td>
								平均评分
							</td>
							<td>
                            	操作
                            </td>
						</tr>
						<c:forEach items="${requestScope.itemProducts}" var="itemProducts1">
							<tr class="row1">
								<td>
									${itemProducts1.name}
								</td>
								<td>
								    <img src="${itemProducts1.imageUrl}" width="88px" height="44px"/>
								</td>
								<td>
									${itemProducts1.score}
								</td>
								<td>
									<a href="${pageContext.request.contextPath}/product/xiangqing?productid=${itemProducts1.productId}">详情</a>&nbsp;
								</td>
							</tr>
						</c:forEach>
					</table>
					<form action="${pageContext.request.contextPath}/product/search" method="post">
                    	<table cellpadding="0" cellspacing="0" border="0"
                    	class="form_table">
                    	<tr>
                    		<td valign="middle" align="right">
                    			查询歌曲:
                    		</td>
                    		<td valign="middle" align="left">
                    		    <input type="text" class="inputgri" name="songName" />
                    		    <input type="submit" class="button" value="查询 &raquo;" />
                    		</td>
                    	</tr>
                    	</table>
                    </form>
				</div>
			</div>
			<div id="footer">
				<div id="footer_bg">
				1165872335@qq.com
				</div>
			</div>
		</div>
	</body>
</html>
