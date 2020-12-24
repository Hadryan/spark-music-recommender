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
                        <input type="button" class="button" value="我的收藏" onclick="location='${pageContext.request.contextPath}/emp/MyShouCang?myName=${sessionScope.user.realname}'"/>
                    </p>
					</div>
					<div id="navigation">
					</div>
				</div>
				<div id="content">
					<p id="whereami">
					</p>
					<h1>
						Welcome ${sessionScope.user.realname}! 查询结果如下：
					</h1>
					<table class="table">
						<tr class="table_header">
							<td>
								歌曲名称
							</td>
							<td>
								播放时长
							</td>
							<td>
								流派类别
							</td>
							<td>
								演唱者
							</td>
							<td>
								作曲家
							</td>
							<td>
                            	作词家
                            </td>
						</tr>
						<c:forEach items="${requestScope.selectSongs}" var="selectSongs">
							<tr class="row1">
								<td>
									${selectSongs.song_name}
								</td>
								<td>
									${selectSongs.song_length}
								</td>
								<td>
									${selectSongs.genre_ids}
								</td>
								<td>
									${selectSongs.artist_name}
								</td>
								<td>
                                	${selectSongs.musician}
                                </td>
                                <td>
                                    ${selectSongs.zuoCijJia}
                                </td>
								<td>
									<a href="${pageContext.request.contextPath}/emp/play?song_id1=${selectSongs.song_id}">播放</a>&nbsp;
									<a href="${pageContext.request.contextPath}/emp/findOne?song_id=${selectSongs.song_id}">收藏</a>
								</td>
							</tr>
						</c:forEach>


					</table>
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
