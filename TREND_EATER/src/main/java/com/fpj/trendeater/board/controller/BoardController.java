package com.fpj.trendeater.board.controller;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.fpj.trendeater.board.exception.BoardException;
import com.fpj.trendeater.board.model.service.BoardService;
import com.fpj.trendeater.board.model.vo.Board;
import com.fpj.trendeater.board.model.vo.PageInfo;
import com.fpj.trendeater.common.Pagination;

@Controller
public class BoardController {

	@Autowired
	private BoardService bService;
	
	
	
	
/********************************************* 게시물 조회 *******************************************************/	
	
	// 게시물 조회 + 페이지네이션
	@RequestMapping("notice.bo") // menubar.jsp의 게시판 버튼의 url주소
	public ModelAndView boardList(@RequestParam(value="page", required=false) Integer page, ModelAndView mv) {
		
		// 넘겨받은 데이터 중에 currentPage가 있으면 currentPage에 받아온 currentPage값을 넣어줘야함
		int currentPage = 1; // currentPage
		
		if(page != null ) { // page가 int라 null 못들어감. 기존 방법이랑 다르게 값 넘어온게 있는지 여부만 체크하고 싶은 상황. 기존 방법은 받아온 값 자체를 체크하는 방식 : if(request.getParameter("currentPage") != null )
		// string으로 받아오면 파싱을 해야되는게 귀찮. 래퍼클래스 Integer 사용하면 모두 해결됨 
			currentPage = page;
		}
		
		// 페이징처리1 :총게시물수 가져오기
		int listCount = bService.getListCount(); 
		// 계산에 필요한 2가지가 갖춰짐 : currentPage, listCount
		
		
		// 페이징처리2 : 필요한 게시판 가져오기
		// PageInfo와 Pagination이 필요한 이유 : 강의 11:07 
		PageInfo pi = Pagination.getPageInfo(currentPage, listCount);
		
		ArrayList<Board> list = bService.getBoardList(pi);
		System.out.println("pi="+pi);
		System.out.println("list="+list); // 항상 디버깅 찍어보기
		
		if(list != null) {
		// model과 ModelAndView 둘 중 하나 선택가능
			mv.addObject("list",list);
			mv.addObject("pi",pi);
			mv.setViewName("board_Notice");
		}else {
			throw new BoardException("게시글 전체 조회에 실패했습니다");
		}
		return mv;
	}
		
		
		
/********************************************* 게시물 : 상세보기 **************************************************/		
	
	
	@RequestMapping("noticeDetail.bo")
	public ModelAndView boardDetail (@RequestParam("bId") int bId, @RequestParam("page") int page, ModelAndView mv) {
		
		Board board = bService.selectBoard(bId);
		
		if(board != null) {
			mv.addObject("board",board).addObject("page",page).setViewName("boardDetailView");
		} else {
			throw new BoardException ("게시글 상세보기에 실패하였습니다.");
		}
		 return mv;
	}

	
	
	
	
/********************************************* 게시물 : 글쓰기 **************************************************/			
	

	
	// 게시판 글쓰기 뷰 불러오기
	@RequestMapping("binsertView.bo")
	public String boardInsertForm() {
		return "boardInsertForm";
	}
	
	
	// 게시판 글쓰기
	@RequestMapping("binsert.bo")
	public String insertBoard(@ModelAttribute Board b, @RequestParam("uploadFile") MultipartFile uploadFile, HttpServletRequest request) {
		// 리퀘스트파램에 받아오는 uploadFile속성은 첨부파일 name속성의 이름
		// 메소드에서 멀티파트파일을 사용할거긴한데, 필드로 올려야하는건 @Autowired 올리는게 맞는데
		// ...은 객체로 만들져서 들어오기 때문에 @Autowired 할 필요가 없는 것 강의3:24
		// 필드에 들어간다는건 객체가 들어간 상태...
//			System.out.println(b);
//			System.out.println(uploadFile);
		
		
		if(uploadFile != null && !uploadFile.isEmpty()) { // 미연의 사고방지를 위한 if조건문
			// jar파일 자체에 잘못이나 인터넷이 안좋다거나 하면 분명 null뜨는 경우가 생김. 만약의 상황의 대비한 if+null조건문 강의 3:31
			// 파일이 들어오면 저장하면 됨
			String renameFileName = saveFile(uploadFile, request); // 여기 request는 HttpServletRequest가 맞음
			
			b.setOriginalFileName(uploadFile.getOriginalFilename());
			b.setRenameFileName(renameFileName);
		}
		
		int result = bService.insertBoard(b);
		
		if(result > 0) {
			return "redirect:blist.bo";
		} else {
			throw new BoardException("게시글 등록에 실패하였습니다.");
		}
	}
		
		
	// 사용자 정의 메소드
	public String saveFile(MultipartFile file, HttpServletRequest request) { // HttpServletRequest으로 Request영역 호출
	
		// 파일 저장 구조 : 파일 저장소가 따로있고 실제로 파일이 저장되는 곳. 이름, 타입 등이 db에 저장되는거였음
		// 프로젝트파일의 저장소 위치 : webapp - resource - buploadFiles
		
		String root = request.getSession().getServletContext().getRealPath("resources"); // application영역으로 가는 코드. 어플영역은 웹앱(웹컨텐트) 아래를 말함
		// request.getSession().getServletContext().getRealPath("resources") -> webapp폴더 아래 resources폴더를 의미함
		// 서블릿컨텍스트까지가 webapp이고 겟리얼패스가 리소스까지 경로를 받아오는 것
		System.out.println("업로드파일 root:"+root);
		String savePath = root + "\\buploadFiles";

		// 업로드된 파일, 파일저장소에 저장
		File folder = new File(savePath);
		if(!folder.exists()) {
			folder.mkdirs(); // 저장할 폴더 생성
		}
		// 저장할 파일명을 변경해야함. 원래는 리네임 규약만들었으나 지금은 그렇게까지는 하지 않겠음. 수업에서
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		// 파일명 랜덤값 만들어서 겹치지 않게 해야하나 이번 수업 때는 생략. 필요하면 jspServlet쪽에 찾아보기를 
		String originFileName = file.getOriginalFilename();
		String renameFileName = sdf.format(new Date(System.currentTimeMillis())) + originFileName.substring(originFileName.lastIndexOf("."));
		
		System.out.println("originFileName = "+originFileName);
		System.out.println("renameFileName = "+renameFileName);
		
		String renamePath = folder + "\\" + renameFileName;
		
		try {
			file.transferTo(new File(renamePath)); // 새로만든 파일을 저정소에 저장 : .transferTo()
			// 겟오리지널파일네임을 사용하면 원래 가지고 있던 파일에 대한 본파일명을 가지고 있으나
			// 리네임된 것 만으로는 업로파일만으로는 못찾음. 세이브파일도 관여하기 때문에 
			// 리네임 파일 네임 컬럼에 넣어야하기 때문에 리네임파일네임을 반환시켜야함. 그래서 리턴타입을 str로 변경 강의 4:42 6.9
			
			// renameFileName 반환하는 이유 
			// saveFile 메소드에서 바뀐이름 파일 정의하고 있음. insertBoard에서 집어 넣게 되는 거니까
			
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return renameFileName;
	}								
	
	
	
	
	
}




