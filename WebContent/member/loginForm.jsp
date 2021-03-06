<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="../member/auth.jsp" %>
<%@ include file="../header.jsp" %>

<!-- 본문 시작 loginForm.jsp -->
<h3>* 로 그 인 *</h3>
<%
if(s_id.equals("guest")||s_passwd.equals("guest")||s_mlevel.equals("guest"))
{	//로그인을 하지 않은 경우
	
	//쿠키값 가져오기-------------------------------------------------------
	Cookie[] cookies=request.getCookies();
	String c_id="";
	if(cookies!=null){	//쿠키 존재하는지?
		for(int idx=0; idx<cookies.length; idx++){
			Cookie cookie=cookies[idx];
			if(cookie.getName().equals("c_id")==true){//쿠키변수 c_id가 있는지?
				c_id=cookie.getValue();
			}
		}//for end
	}//if end
	//------------------------------------------------------------------------

%>
<form name="loginFrm" method="post" action="loginProc.jsp" onsubmit="return loginCheck(this)">
	<table>
	<tr>
		<td>
			<input type="text" name="id" id="id" value="<%=c_id %>" placeholder="아이디" required>
		</td>
		<td rowspan="2">
			<input type="image" src="../images/bt_login.gif" style="cursor:pointer">
		</td>
	</tr>
	<tr>
		<td>
			<input type="password" name="passwd" id="passwd" placeholder="비밀번호" required>
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<p>
				<input type="checkbox" name="c_id" value="SAVE" <%if(!(c_id.isEmpty())){out.println("checked");} %>>아이디저장
				&nbsp;&nbsp;
				<a href = "agreement.jsp">회원가입</a>
				&nbsp;&nbsp;
				<a href = "Search_idpw.jsp">아이디/비번찾기</a>
			</p>
		</td>
	</tr>
	</table>
</form>
<%
}else{
	//로그인 성공했다면
	out.println("<strong>"+s_id+"</strong>님");
	out.println("<a href='logout.jsp'>[로그아웃]</a>");
	out.println("<br><br>");
	out.println("<a href='memUpdate.jsp'>[회원 정보 수정]</a>");
	out.println("<a href='memDelete.jsp'>[회원 탈퇴]</a>");
}//if end
%>


<!-- 본문 끝 -->

<%@ include file="../footer.jsp" %>