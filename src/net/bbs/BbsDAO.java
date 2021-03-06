package net.bbs;

import java.sql.*;
import java.util.ArrayList;

import net.utility.*;

public class BbsDAO {
	private DBOpen	dbopen=null;
	private DBClose dbclose=null;
	
	private Connection con=null;
	private PreparedStatement pstmt=null;
	private ResultSet rs=null;
	private StringBuilder sql=null;
	
	public BbsDAO() {
		dbopen = new DBOpen();
		dbclose = new DBClose();
	}
	
	public int insert(BbsDTO dto) {
		Connection con=null;
		PreparedStatement pstmt=null;
		StringBuilder sql=null;
		int res=0;
		try {
			con=dbopen.getConnection();	//DB연결
			
			sql = new StringBuilder();
			sql.append(" INSERT INTO  tb_bbs(bbsno, wname, subject, content, passwd, grpno, ip, regdt)");
			sql.append(" VALUES((SELECT NVL(MAX(bbsno), 0)+1 FROM tb_bbs), ?, ?, ?, ?, (SELECT NVL(MAX(bbsno), 0)+1 FROM tb_bbs), ?, sysdate)");
			
			pstmt = con.prepareStatement(sql.toString());
			pstmt.setString(1, dto.getWname());
			pstmt.setString(2, dto.getSubject());
			pstmt.setString(3, dto.getContent());
			pstmt.setString(4, dto.getPasswd());
			pstmt.setString(5, dto.getIp());
			
			res=pstmt.executeUpdate();
			
		}catch(Exception e) {
			System.out.println("추가실패 : "+e);
		}finally {
			dbclose.close(con, pstmt);
		}//try end
		
		return res;
	}//insert() end
	
	
	public synchronized ArrayList<BbsDTO> list() {
		Connection con=null;
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		StringBuilder sql=null;
		ArrayList<BbsDTO> list=null;
		
		try {
			con=dbopen.getConnection();
			sql=new StringBuilder();
			sql.append(" SELECT bbsno, wname, subject, readcnt, indent, grpno, regdt");
			sql.append(" FROM tb_bbs");
			sql.append(" ORDER BY grpno DESC, ansnum ASC");
			
			pstmt=con.prepareStatement(sql.toString());
			rs=pstmt.executeQuery();
			if(rs.next()) {
				list=new ArrayList<>();		//전체저장
				do {
					BbsDTO dto=new BbsDTO();	//한줄저장
					dto.setBbsno(rs.getInt("bbsno"));
					dto.setWname(rs.getString("wname"));
					dto.setSubject(rs.getString("subject"));
					dto.setReadcnt(rs.getInt("readcnt"));
					dto.setIndent(rs.getInt("indent"));
					dto.setGrpno(rs.getInt("grpno"));
					dto.setRegdt(rs.getString("regdt"));
					list.add(dto);
				}while(rs.next());
			}//if end
			
		}catch(Exception e) {
			System.out.println("목록실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
		
		return list;
		
	}//list() end
	
	public BbsDTO read(int bbsno) {
		Connection con=null;
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		StringBuilder sql=null;
		BbsDTO dto=null;
		
		try {
			incrementCnt(bbsno); //조회수 증가
			con=dbopen.getConnection();
			sql=new StringBuilder();
			sql.append(" SELECT bbsno, wname, subject, content, readcnt, grpno, ip, regdt");
			sql.append(" FROM tb_bbs");
			sql.append(" WHERE bbsno=?");
			
			pstmt=con.prepareStatement(sql.toString());
			pstmt.setInt(1, bbsno);
			rs=pstmt.executeQuery();
			if(rs.next()) {
				dto=new BbsDTO();
				dto.setBbsno(rs.getInt("bbsno"));
				dto.setWname(rs.getString("wname"));
				dto.setSubject(rs.getString("subject"));
				dto.setContent(rs.getString("content"));
				dto.setReadcnt(rs.getInt("readcnt"));
				dto.setGrpno(rs.getInt("grpno"));
				dto.setIp(rs.getString("ip"));
				dto.setRegdt(rs.getString("regdt"));
			}
		}catch(Exception e) {
			System.out.println("상세보기 실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
		
		return dto;
		
	}//read() end
	
	 //조회수 증가
	 public void incrementCnt(int bbsno){
		 Connection con=null;
		 PreparedStatement pstmt=null;    
		 StringBuffer sql=null;
	    
		 try {
			 con=dbopen.getConnection();
			 sql=new StringBuffer();
			 sql.append(" UPDATE tb_bbs");
			 sql.append(" SET readcnt=readcnt+1");
			 sql.append(" WHERE bbsno=?");
			 pstmt=con.prepareStatement(sql.toString());
			 pstmt.setInt(1, bbsno);
			 pstmt.executeUpdate();
		 }catch(Exception e){
			 System.out.println(e);
		 }finally{
			 dbclose.close(con, pstmt);
		 }
	}//incrementCnt() end
	 
	 public int reply(BbsDTO dto) {
		 int res=0;
		 try {
			 con=dbopen.getConnection();
			 sql=new StringBuilder();
			 
			 //1) 부모글정보 가져오기(그룹번호, 들여쓰기, 글순서)
			 int grpno=0, indent=0, ansnum=0;
			 sql.append(" SELECT grpno, indent, ansnum ");
			 sql.append(" FROM tb_bbs");
			 sql.append(" WHERE bbsno=?");
			 pstmt=con.prepareStatement(sql.toString());
			 pstmt.setInt(1, dto.getBbsno());
			 rs=pstmt.executeQuery();
			 if(rs.next()) {
				 grpno=rs.getInt("grpno");				//부모글 그룹번호
				 indent=rs.getInt("indent")+1;			//부모글 들여쓰기+1
				 ansnum=rs.getInt("ansnum")+1;	//부모글 글순서+1
			 }//if end
			 
			 //2) 글순서 재조정
			 sql.delete(0, sql.length());	//1)에서 사용했던 sql문 지우기
			 sql.append(" UPDATE tb_bbs");
			 sql.append(" SET ansnum=ansnum+1");
			 sql.append(" WHERE grpno=? AND ansnum>=?");
			 pstmt=con.prepareStatement(sql.toString());
			 pstmt.setInt(1, grpno);
			 pstmt.setInt(2, ansnum);
			 pstmt.executeUpdate();
			 
			 //3) 답변글 추가
			 sql.delete(0, sql.length());
			 sql.append(" INSERT INTO tb_bbs");
			 sql.append("(bbsno, wname, subject, content, passwd, ip, grpno, indent, ansnum) ");
			 sql.append(" values((SELECT NVL(MAX(bbsno), 0)+1 FROM tb_bbs)");
			 sql.append(" , ?, ?, ?, ?, ?, ?, ?, ?) ");
			 pstmt=con.prepareStatement(sql.toString());
			 pstmt.setString(1, dto.getWname());
			 pstmt.setString(2, dto.getSubject());
			 pstmt.setString(3, dto.getContent());
			 pstmt.setString(4, dto.getPasswd());
			 pstmt.setString(5, dto.getIp());
			 pstmt.setInt(6, grpno);
			 pstmt.setInt(7, indent);
			 pstmt.setInt(8, ansnum);
			 res = pstmt.executeUpdate();
		 }
		 catch(Exception e){
			 System.out.println("답변실패 : "+e);
		 }finally{
			 dbclose.close(con, pstmt, rs);
		 }
		 return res;
	 }//reply() end
	 
	public int delete(BbsDTO dto) {
		Connection con=null;
		PreparedStatement pstmt=null;
		StringBuilder sql=null;
		int res=0;
		try {
			con=dbopen.getConnection();	//DB연결
				
			sql = new StringBuilder();
			sql.append(" DELETE FROM tb_bbs");
			sql.append(" WHERE passwd=? AND bbsno=?");
				
			pstmt = con.prepareStatement(sql.toString());
			pstmt.setString(1, dto.getPasswd());
			pstmt.setInt(2, dto.getBbsno());
				
			res=pstmt.executeUpdate();
				
		}catch(Exception e) {
			System.out.println("삭제실패 : "+e);
		}finally {
			dbclose.close(con, pstmt);
		}//try end
			
		return res;
	}//delete() end
	
	public BbsDTO updateform(BbsDTO dto) {
		Connection con=null;
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		StringBuilder sql=null;
		//BbsDTO dto=null;
		
		try {
			con=dbopen.getConnection();
			
			sql = new StringBuilder();
			sql.append(" SELECT bbsno, wname, subject, content, passwd, ip");
			sql.append(" FROM tb_bbs");
			sql.append(" WHERE passwd=? AND bbsno=?");
				
			pstmt = con.prepareStatement(sql.toString());
			pstmt.setString(1, dto.getPasswd());
			pstmt.setInt(2, dto.getBbsno());
				
			rs=pstmt.executeQuery();
			if(rs.next()) {
				dto=new BbsDTO();
				dto.setBbsno(rs.getInt("bbsno"));
				dto.setWname(rs.getString("wname"));
				dto.setSubject(rs.getString("subject"));
				dto.setContent(rs.getString("content"));
				dto.setPasswd(rs.getString("passwd"));
				dto.setIp(rs.getString("ip"));
			}else {
				dto=null;
			}
		}catch(Exception e) {
			System.out.println("실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
			
		return dto;
		
	}//updateform() end
	
	public int update(BbsDTO dto) {
		Connection con=null;
		PreparedStatement pstmt=null;
		StringBuilder sql=null;
		int res=0;
		
		try {
			con=dbopen.getConnection();
			
			sql = new StringBuilder();
			sql.append(" UPDATE tb_bbs");
			sql.append(" SET wname=?, subject=?, content=?, passwd=?, ip=?");
			sql.append(" WHERE bbsno=?");
				
			pstmt = con.prepareStatement(sql.toString());
			pstmt.setString(1, dto.getWname());
			pstmt.setString(2, dto.getSubject());
			pstmt.setString(3, dto.getContent());
			pstmt.setString(4, dto.getPasswd());
			pstmt.setString(5, dto.getIp());
			pstmt.setInt(6, dto.getBbsno());

			res=pstmt.executeUpdate();
			
		}catch(Exception e) {
			System.out.println("수정실패 : "+e);
		}finally {
			dbclose.close(con, pstmt);
		}//try end
		
		return res;
	}//update() end
	
	public int count(String col, String word) {
		int cnt=0;
		try {
			con=dbopen.getConnection();
			sql = new StringBuilder();
			sql.append(" SELECT COUNT(*) AS CNT ");
			sql.append(" FROM tb_bbs ");
			
			if(word.length()>=1) {	//검색어가 있는지?
				String search="";
				if(col.equals("wname")) {
					search += " WHERE wname LIKE '%"+word+"%'";
				}else if(col.equals("subject")) {
					search += " WHERE subject LIKE '%"+word+"%'";
				}else if(col.equals("content")) {
					search += " WHERE content LIKE '%"+word+"%'";
				}
				else if(col.equals("subject_content")) {
					search += " WHERE subject LIKE '%"+word+"%'";
					search += " OR content LIKE '%"+word+"%'";
				}
				sql.append(search);
			}//if end
			
			pstmt=con.prepareStatement(sql.toString());
			rs=pstmt.executeQuery();
			if(rs.next()) {
				cnt=rs.getInt("cnt");
			}
		}catch(Exception e) {
			System.out.println("글갯수 실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
		return cnt;
		
	}//count() end
	
	
	public synchronized ArrayList<BbsDTO> list(String col, String word) {
		Connection con=null;
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		StringBuilder sql=null;
		ArrayList<BbsDTO> list=null;
		
		try {
			con=dbopen.getConnection();
			sql=new StringBuilder();
			sql.append(" SELECT bbsno, wname, subject, readcnt, indent, grpno, regdt");
			sql.append(" FROM tb_bbs");
			
			if(word.length()>=1) {	//검색어가 있는지?
				String search="";
				if(col.equals("wname")) {
					search += " WHERE wname LIKE '%"+word+"%'";
				}else if(col.equals("subject")) {
					search += " WHERE subject LIKE '%"+word+"%'";
				}else if(col.equals("content")) {
					search += " WHERE content LIKE '%"+word+"%'";
				}
				else if(col.equals("subject_content")) {
					search += " WHERE subject LIKE '%"+word+"%'";
					search += " OR content LIKE '%"+word+"%'";
				}
				sql.append(search);
			}//if end
			
			sql.append(" ORDER BY grpno DESC, ansnum ASC");
			
			pstmt=con.prepareStatement(sql.toString());
			rs=pstmt.executeQuery();
			if(rs.next()) {
				list=new ArrayList<>();		//전체저장
				do {
					BbsDTO dto=new BbsDTO();	//한줄저장
					dto.setBbsno(rs.getInt("bbsno"));
					dto.setWname(rs.getString("wname"));
					dto.setSubject(rs.getString("subject"));
					dto.setReadcnt(rs.getInt("readcnt"));
					dto.setIndent(rs.getInt("indent"));
					dto.setGrpno(rs.getInt("grpno"));
					dto.setRegdt(rs.getString("regdt"));
					list.add(dto);
				}while(rs.next());
			}//if end
			
		}catch(Exception e) {
			System.out.println("검색 목록 실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
		
		return list;
		
	}//list() end
	
	public ArrayList<BbsDTO> list(String col, String word, int nowPage, int recordPerPage) {
	    ArrayList<BbsDTO> list = null;

	    // 10: 페이지당 출력할 레코드 갯수
	    int startRow = ((nowPage-1) * recordPerPage) + 1; // (0 * 10) + 1 = 1, 11, 21
	    int endRow = nowPage * recordPerPage;             // 1 * 10 = 10, 20, 30
	    
	    /*
	     1 page: WHERE r >= 1 AND r <= 10;
	     2 page: WHERE r >= 11 AND r <= 20;
	     3 page: WHERE r >= 21 AND r <= 30;
	     */
	    
	    
	    try {
	    	con=dbopen.getConnection();
			sql=new StringBuilder();
	      
	      word = word.trim(); // 문자열 좌우 공백 제거
	      
	      if (word.length() == 0){ // 검색을 안하는 경우
	        sql.append(" SELECT bbsno, wname, subject, readcnt, regdt, grpno, indent, ansnum, r");
	        sql.append(" FROM(");
	        sql.append("      SELECT bbsno, wname, subject, readcnt, regdt, grpno, indent, ansnum, rownum as r");
	        sql.append("      FROM (");
	        sql.append("           SELECT bbsno, wname, subject, readcnt, regdt, grpno, indent, ansnum");
	        sql.append("           FROM tb_bbs ");
	        //sql.append("           WHERE indent=0 ");
	        sql.append("           ORDER BY grpno DESC, ansnum ASC");
	        sql.append("      )");
	        sql.append(" )     ");
	        sql.append(" WHERE r >= "+startRow+" AND r <= "+endRow);
	        
	        pstmt = con.prepareStatement(sql.toString());
	        
	      }
	      else{ // 검색을 하는 경우
	        sql.append(" SELECT bbsno, wname, subject, readcnt, regdt, grpno, indent, ansnum, r");
	        sql.append(" FROM(");
	        sql.append("      SELECT bbsno, wname, subject, readcnt, regdt, grpno, indent, ansnum, rownum as r");
	        sql.append("      FROM (");
	        sql.append("           SELECT bbsno, wname, subject, readcnt, regdt, grpno, indent, ansnum");
	        sql.append("           FROM tb_bbs ");
	        
	        //검색
	        if(word.length()>=1){
	          String search=" WHERE "+col+" LIKE '%"+word+"%' ";	// AND indent=0
	          sql.append(search);
	        } 
	        
	        sql.append("           ORDER BY grpno DESC, ansnum ASC");
	        sql.append("      )");
	        sql.append(" )     ");
	        sql.append(" WHERE r >= "+startRow+" AND r <= "+endRow);
	        
	        pstmt = con.prepareStatement(sql.toString());

	      }
	      
	      rs=pstmt.executeQuery();
	      if(rs.next()) {
	        list=new ArrayList<BbsDTO>();
	        BbsDTO dto=null; //레코드 1개보관
	        do {
	          dto=new BbsDTO();
	          dto.setBbsno(rs.getInt("bbsno"));
	          dto.setSubject(rs.getString("subject"));
	          dto.setReadcnt(rs.getInt("readcnt"));
	          dto.setWname(rs.getString("wname"));
	          dto.setRegdt(rs.getString("regdt"));
	          dto.setGrpno(rs.getInt("grpno"));
	          dto.setIndent(rs.getInt("indent"));
	          dto.setAnsnum(rs.getInt("ansnum"));
	          list.add(dto);
	        }while(rs.next());
	      }

	    } catch (Exception e) {
	      System.out.println(e.toString());
	    } finally {
	      dbclose.close(con, pstmt, rs);
	    }

	    return list;
	    
	  } // list() end
	
/*	public void replyCnt(int grpno, int indent, ) {
		
	}//replyCnt() end
*/	
	
	public ArrayList<BbsDTO> comment(String col, String word, int nowPage, int recordPerPage) {
		Connection con = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    ArrayList<BbsDTO> comment = null;
	    
		int startRow = ((nowPage-1) * recordPerPage) + 1;
	    int endRow = nowPage * recordPerPage;

		try {
			con=dbopen.getConnection();
			sql=new StringBuilder();
			
			word = word.trim(); // 문자열 좌우 공백 제거
			if (word.length() == 0){ // 검색을 안하는 경우
				sql.append(" SELECT bbsno, subject, wname, readcnt, regdt, grpno, cnt, rnum");
				sql.append(" FROM (");
				sql.append("	SELECT bbsno, subject, wname, readcnt, regdt, grpno, cnt, rownum as rnum");
				sql.append("	FROM (");
				sql.append("		SELECT bb.bbsno, bb.subject, bb.wname, bb.readcnt, aa.grpno, aa.cnt, bb.regdt");
				sql.append("		FROM(");
				sql.append("			SELECT grpno, COUNT(grpno)-1 as cnt");
				sql.append("			FROM tb_bbs bb");
				sql.append("			GROUP BY grpno");
				sql.append("			) aa JOIN tb_bbs bb");
				sql.append("		ON aa.grpno=bb.grpno");
				sql.append("		WHERE bb.indent=0");
				sql.append("		ORDER BY grpno DESC");
				sql.append("	)");
				sql.append(" )");
				sql.append(" WHERE rnum >= "+startRow+" AND rnum <= "+endRow +" AND cnt!=0");
	
				pstmt=con.prepareStatement(sql.toString());
			
			}else {
				sql.append(" SELECT bbsno, subject, grpno, cnt, wname, readcnt, regdt,  rnum ");
				sql.append(" FROM ( ");
				sql.append(" 	SELECT bbsno, subject, wname, readcnt, regdt, grpno, cnt, rownum as rnum ");
				sql.append(" FROM ( ");
				sql.append(" 		SELECT bb.bbsno, bb.subject, bb.wname, bb.readcnt, aa.grpno, aa.cnt, bb.regdt ");
				sql.append(" 		FROM( ");
				sql.append(" 			SELECT grpno, COUNT(grpno)-1 as cnt ");
				sql.append(" 			FROM tb_bbs ");
		        
		        //검색
		        if(word.length()>=1){
			        String search=" WHERE "+col+" LIKE '%"+word+"%' ";
			        sql.append(search);
		        } 
		        
		        sql.append("			GROUP BY grpno ");
		        sql.append("			) aa JOIN tb_bbs bb ");
		        sql.append("		ON aa.grpno=bb.grpno ");
		        sql.append("		WHERE bb.indent=0 ");
		        sql.append("		ORDER BY grpno DESC "); 
		        sql.append("	) ");
		        sql.append(") ");
		        sql.append(" WHERE rnum >= "+startRow+" AND rnum<= "+endRow+" AND cnt!=0" );
		        
		        pstmt = con.prepareStatement(sql.toString());

		    }
			
			rs=pstmt.executeQuery();
			if(rs.next()) {
				comment=new ArrayList<>();		//전체저장
				do {
					BbsDTO dto=new BbsDTO();	//한줄저장
					dto.setBbsno(rs.getInt("bbsno"));
					dto.setSubject(rs.getString("subject"));
					dto.setGrpno(rs.getInt("grpno"));
					dto.setComment(rs.getInt("cnt"));
					dto.setReadcnt(rs.getInt("readcnt"));
					dto.setWname(rs.getString("wname"));
					dto.setRegdt(rs.getString("regdt"));
					comment.add(dto);
				}while(rs.next());
			}//if end
		}catch (Exception e) {
		      System.out.println(e.toString());
		}finally {
		     dbclose.close(con, pstmt, rs);
		}
		return comment;
	}//comment() end
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//관리자
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	public synchronized ArrayList<BbsDTO> bslist(String col) {
		ArrayList<BbsDTO> bslist=null;
		
		try {
			con=dbopen.getConnection();
			sql=new StringBuilder();
			sql.append(" SELECT bbsno, wname, subject, readcnt, indent, grpno, regdt");
			sql.append(" FROM tb_bbs");
			
			if(col.equals("bbsno")||col.equals("")) {
				sql.append(" ORDER BY bbsno DESC");
			}else if(col.equals("subject")) {
				sql.append(" ORDER BY subject ASC");
			}else if(col.equals("regdt")) {
				sql.append(" ORDER BY regdt DESC");
			}//if end
			
			pstmt=con.prepareStatement(sql.toString());
			rs=pstmt.executeQuery();
			if(rs.next()) {
				bslist=new ArrayList<>();		//전체저장
				do {
					BbsDTO dto=new BbsDTO();	//한줄저장
					dto.setBbsno(rs.getInt("bbsno"));
					dto.setWname(rs.getString("wname"));
					dto.setSubject(rs.getString("subject"));
					dto.setReadcnt(rs.getInt("readcnt"));
					dto.setIndent(rs.getInt("indent"));
					dto.setGrpno(rs.getInt("grpno"));
					dto.setRegdt(rs.getString("regdt"));
					bslist.add(dto);
				}while(rs.next());
			}//if end
			
		}catch(Exception e) {
			System.out.println("목록실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
		
		return bslist;
		
	}//list() end
	
	
	public int recordCount() {	//게시글 카운트
		int cnt=0;
		try {
			con=dbopen.getConnection();
			sql=new StringBuilder();
			sql.append(" SELECT COUNT(bbsno) cnt");
			sql.append(" FROM tb_bbs");
			
			pstmt=con.prepareStatement(sql.toString());
			rs=pstmt.executeQuery();
			if(rs.next()) {
				cnt=rs.getInt("cnt");
			}else {
				
			}
		}catch(Exception e) {
			System.out.println("글 조회 실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
		return cnt;
	}//recordCount() end///////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	public int bsDel(String checkRow[]) {
		int res=0;
		try{
			con=dbopen.getConnection();
			sql=new StringBuilder();
			String str="";
			str+="DELETE FROM tb_bbs WHERE bbsno IN ("+checkRow[0];
			if(checkRow.length>1) {
				for(int i=1; i<checkRow.length; i++ ) {
					str+=","+checkRow[i];
				}//for end
			}//if end
			str+=")";
			sql.append(str);
			pstmt=con.prepareStatement(sql.toString());
			res=pstmt.executeUpdate();
		}catch(Exception e) {
			System.out.println("삭제 실패 : "+e);
		}finally {
			dbclose.close(con, pstmt, rs);
		}//try end
		return res;
	}//bsDel() end///////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	private DBOpen getConnection() {
		// TODO Auto-generated method stub
		return null;
	}

}//class end
