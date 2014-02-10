package jon.bookstore.user;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jon.mail.Mail;
import jon.mail.MailUtils;
import jon.servlet.BaseServlet;
import jon.utils.CommonUtils;

public class UserServlet extends BaseServlet {
	private UserService userService = new UserService();
	public String registPre(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		return "/jsps/regist.jsp";
	}
	
	public String loginPre(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		return "/jsps/login.jsp";
	}
	
	public String regist(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 把表单数据封装到User对象中
		final User user = CommonUtils.toBean(request.getParameterMap(), User.class);
		user.setUid(CommonUtils.uuid());
		user.setState(false);
		user.setCode(CommonUtils.uuid() + CommonUtils.uuid());
		
		// 完成注册
		try {
			userService.regist(user);
		} catch (UserException e) {
			String msg = e.getMessage();//获取异常信息
			request.setAttribute("msg", msg);
			request.setAttribute("user", user);
			return "/jsps/regist.jsp";
		}
		
		final String url = "http://localhost:8080/bookstore/UserServlet?method=active&code="
				+ user.getCode();
		
		new Thread() {
			public void run() {
				Mail mail = new Mail();
				mail.setFrom("joncai2012@163.com");
				mail.addToAddress(user.getEmail());
				mail.setSubject("来自Jon超级图书商城的激活邮件");
				mail.setContent("点击<a href='" + url + "'>这里</a>完成激活~！");
				
				Session session = MailUtils.createSession("smtp.163.com", "joncai2012@163.com", ";lkj0987");
				try {
					MailUtils.send(session, mail);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}.start();
		
		request.setAttribute("msg", "注册成功！请马上去邮箱激活，若没有立刻受到邮件，说明您网速有问题！");
		List<String> links = new ArrayList<String>();
		links.add("<a href='" + request.getContextPath() + "/index.jsp'>主页</a>");
		links.add("<a href='" + request.getContextPath() + "/jsps/login.jsp'>登录</a>");
		request.setAttribute("links", links);
		return "/jsps/msg.jsp";
	}
	
	public String active(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/*
		 * 获取激活码
		 * 查询数据库，查出来一个User对象
		 */
		String code = request.getParameter("code");
		User user = userService.findByCode(code);
		if (user == null) {
			request.setAttribute("msg", "无效的激活码！");
			List<String> links = new ArrayList<String>();
			links.add("<a href='" + request.getContextPath() + "/index.jsp'>主页</a>");
			links.add("<a href='" + request.getContextPath() + "/jsps/regist.jsp'>注册</a>");
			request.setAttribute("links", links);
			return "/jsps/msg.jsp";
		}
		if (user.isState()) {
			response.sendError(500, "您已经激活，无需重复激活！");
			return null;
		}
		
		// 修改状态
		request.setAttribute("msg", "恭喜！您已激活成功！");
		List<String> links = new ArrayList<String>();
		links.add("<a href='" + request.getContextPath() + "/index.jsp'>主页</a>");
		links.add("<a href='" + request.getContextPath()
				+ "/jsps/login.jsp'>登录</a>");
		request.setAttribute("links", links);
		return "/jsps/msg.jsp";
	}
}
