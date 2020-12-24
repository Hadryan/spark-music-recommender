<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page  contentType="text/html; UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<title>update Emp</title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<link rel="stylesheet" type="text/css"
			href="${pageContext.request.contextPath}/ems/css/style.css" />
	</head>

	<body>
		<div id="wrap">
			<div id="top_content">
					<div id="header">
						<div id="rightheader">
							<p>
								2020/3/24
								<br />
							</p>
						</div>
						<div id="topheader">
							<h1 id="title">
								<a href="#">小虎SpringBoot入门</a>
							</h1>
						</div>
						<div id="navigation">
						</div>
					</div>
				<div id="content">
					<p id="whereami">
					</p>
					<h1>
						update Emp info:
					</h1>
					<form action="${pageContext.request.contextPath}/emp/ShouCang?realname=${sessionScope.user.realname}" method="post">
						<table cellpadding="0" cellspacing="0" border="0"
							class="form_table">
							<tr>
								<td valign="middle" align="right">
									歌曲名称:
								</td>
								<td valign="middle" align="left">
									${requestScope.song.song_name}
								</td>
							</tr>
							<tr>
								<td valign="middle" align="right">
									歌曲时长:
								</td>
								<td valign="middle" align="left">
									${requestScope.song.song_length}
								</td>
							</tr>
							<tr>
								<td valign="middle" align="right">
									歌曲流派:
								</td>
								<td valign="middle" align="left">
									${requestScope.song.genre_ids}
								</td>
							</tr>
							<tr>
                            	<td valign="middle" align="right">
                            		演唱者:
                                </td>
                            	<td valign="middle" align="left">
                            	    ${requestScope.song.artist_name}
                            	</td>
                            </tr>
                            <tr>
                                <td valign="middle" align="right">
                                    作曲家:
                                </td>
                                <td valign="middle" align="left">
                                    ${requestScope.song.musician}
                                </td>
                            </tr>
                            <tr>
                                <td valign="middle" align="right">
                                    作词家:
                                </td>
                                <td valign="middle" align="left">
                                     ${requestScope.song.zuoCijJia}
                                </td>
                            </tr>
                            <tr>
                                <td valign="middle" align="right">
                                    语言:
                                </td>
                                <td valign="middle" align="left">
                                    ${requestScope.song.language}
                                </td>
                            </tr>
						</table>
						<p>
							<input type="submit" class="button" value="确认收藏" />
						</p>
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
