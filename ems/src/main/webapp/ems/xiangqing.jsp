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
						Welcome ${sessionScope.user.username}! 当前音乐 ${requestScope.product.name} 详情如下：
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
                            	分类
                            </td>
                            <td>
                                用户标签
                            </td>
                            <td>
                                基于内容推荐猜你喜欢
                            </td>
                            <td>
                                基于ItemCF推荐猜你喜欢
                            </td>
						</tr>
						<tr class="row1">
							<td>
								${requestScope.product.name}
							</td>
							<td>
								<img src="${requestScope.product.imageUrl}" width="200px" height="300px"/>
							</td>
							<td>
								${requestScope.product.score}
							</td>
							<td>
								${requestScope.product.categories}
							</td>
							<td>
                                ${requestScope.product.tags}
                            </td>
                            <td>
                                <a href="${pageContext.request.contextPath}/product/neirong?pid1=${requestScope.product.productId}">基于内容推荐</a>&nbsp;
                            </td>
                            <td>
                                <a href="${pageContext.request.contextPath}/product/itemcf?pid2=${requestScope.product.productId}">基于ItemCF推荐</a>&nbsp;
                            </td>
						</tr>
					</table>
					<form action="${pageContext.request.contextPath}/product/dafen" method="post">
                        <table cellpadding="0" cellspacing="0" border="0"
                            class="form_table">
                            <tr>
                            <tr>
                            	<td valign="middle" align="right">
                            		id:
                            	</td>
                            	<td valign="middle" align="left">
                            		${requestScope.product.productId}
                            	    <input type="hidden"  name="id" value="${requestScope.product.productId}">
                            	</td>
                            	</tr>
                                <td valign="middle" align="right">
                                    根据自己喜好给该商品打分（0~5分）:
                                </td>
                                <td valign="middle" align="left">
                                    <input type="text" class="inputgri" name="pingfen" />
                                    <input type="submit" class="button" value="打分 &raquo;" />
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
