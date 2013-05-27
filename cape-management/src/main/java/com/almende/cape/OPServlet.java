package com.almende.cape;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.openid4java.association.AssociationException;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.DirectError;
import org.openid4java.message.IndirectError;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;
import org.openid4java.server.ServerException;
import org.openid4java.server.ServerManager;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.util.Base64;

public class OPServlet extends HttpServlet implements Servlet {
	private static final long		serialVersionUID	= -7678630854605957869L;
	private ServerManager			manager;
	private static final String		OPENID_URI			= "/cape_mgmt/openid/";
	private static final String		SERVER_URI			= "/cape_mgmt/provider/server/o2";
	private static LDAPConnection	conn				= null;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		String passwd = config.getInitParameter("ldap_passwd");
		LDAP.setPasswd(passwd);
		
		try {
			conn = LDAP.get();
		} catch (LDAPException e) {
			throw new ServletException(
					"Sorry, couldn't connect to LDAP server", e);
		}
		
		manager = new ServerManager();
		manager.getRealmVerifier().setEnforceRpId(true);
		
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String domain = req.getServerName();
		
		HttpSession session = req.getSession(true);
		Map<String, String[]> paramsMap = new HashMap<String, String[]>();
		if (session.getAttribute("paramsMap") != null) {
			paramsMap.putAll((Map<String, String[]>) session
					.getAttribute("paramsMap"));
		}
		paramsMap.putAll(req.getParameterMap());
		
		Message response = null;
		String responseText = "";
		
		// If request is for XRDS, return XRDS document
		String uri = req.getRequestURI();
		if (uri == null) {
			response = DirectError.createDirectError("Empty request?");
			responseText = response.keyValueFormEncoding();
			directResponse(resp, responseText);
			return;
		}
		if (uri.startsWith(OPENID_URI)) { // Our openId id-urls have the form of
											// cape_mgmt/openid/<username>
			resp.setContentType("application/xrds+xml");
			directResponse(resp, writeXRDS(domain));
			return;
		} else if (!uri.equals(SERVER_URI)) {
			response = DirectError
					.createDirectError("Unknown endpoint requested: '" + uri
							+ "'");
			responseText = response.keyValueFormEncoding();
			directResponse(resp, responseText);
			return;
		}
		
		String mode = paramsMap.containsKey("openid.mode") ? paramsMap
				.get("openid.mode")[0] : null;
		
		ParameterList params = new ParameterList(req.getParameterMap());
		if ("associate".equals(mode)) {
			// process an association request
			response = manager.associationResponse(params);
			responseText = response.keyValueFormEncoding();
		} else if ("checkid_setup".equals(mode)
				|| "checkid_immediate".equals(mode)) {
			
			Boolean isAuthenticated = ((Boolean) session
					.getAttribute("isAuthenticated"));
			if (isAuthenticated == null) {
				isAuthenticated = false;
			}
			
			String userSelectedId = "";
			if (paramsMap.containsKey("openid.identity")){
				 userSelectedId = paramsMap.get("openid.identity")[0];
			} else {
				directResponse(resp, "openid.identity is missing!");
				return;
			}
			
			String authorization = req.getHeader("Authorization");
			String[] values = null;
			
			if (authorization != null && authorization.startsWith("Basic")) {
				// Authorization: Basic base64credentials
				String base64Credentials = authorization.substring(
						"Basic".length()).trim();
				String credentials = new String(
						Base64.decode(base64Credentials),
						Charset.forName("UTF-8"));
				// credentials = username:password
				values = credentials.split(":", 2);
				isAuthenticated = false;
			}
			
			if (values != null && !userSelectedId.endsWith("/" + values[0])) {
				// Incorrect user used for authentication, restart session!
				session.invalidate();
				session = req.getSession(true);
				values = null;
			}
			LDAPEntry entry = null;
			if (values != null) {
				// Create dn, using hostname & username:
				String dn = "uid=" + values[0] + ",ou=" + domain
						+ "_Users,dc=cape,dc=almende,dc=org";
				try {
					entry = conn.read(dn);
					String passwd = "{MD5}"
							+ Base64.encode(DigestUtils.md5(values[1]));
					if (entry.getAttribute("userPassword").getStringValue()
							.equals(passwd)) {
						isAuthenticated = true;
					}
				} catch (LDAPException e1) {
					if (LDAPException.NO_SUCH_OBJECT != e1.getResultCode()) {
						throw new ServletException(e1);
					}
				}
			}
			if (!isAuthenticated) {
				// retry
				session.invalidate();
				session = req.getSession(true);
				session.setAttribute("paramsMap", paramsMap);
				resp.setStatus(401);
				resp.setHeader("WWW-Authenticate",
						"basic realm=\"CAPE openId\"");
				resp.getWriter().println(
						"Login Required, unknown user or password given.");
				return;
			}
			String SERVER_ENDPOINT = "http://" + domain + ":8080" + SERVER_URI;
			String userSelectedClaimedId = params
					.getParameterValue("openid.claimed_id");
			
			try {
				AuthRequest authReq = AuthRequest.createAuthRequest(params,
						manager.getRealmVerifier());
				
				response = manager.authResponse(params, userSelectedId,
						userSelectedClaimedId, true, SERVER_ENDPOINT);
				if (response instanceof DirectError) {
					directResponse(resp, response.keyValueFormEncoding());
					return;
				} else if (response instanceof IndirectError) {
					directResponse(resp, response.keyValueFormEncoding());
					return;
				} else {
					isAuthenticated = true;
					if (entry != null) {
						MessageExtension axExt = null;
						MessageExtension SRegExt = null;
						if (authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
							axExt = authReq
									.getExtension(AxMessage.OPENID_NS_AX);
						}
						if (authReq.hasExtension(SRegMessage.OPENID_NS_SREG)) {
							SRegExt = authReq
									.getExtension(SRegMessage.OPENID_NS_SREG);
						}
						Map<String, String> required = new HashMap<String, String>();
						Map<String, String> optional = new HashMap<String, String>();
						FetchRequest fetchReq = null;
						List<String> SRegRequired = new ArrayList<String>();
						List<String> SRegOptional = new ArrayList<String>();
						SRegRequest sregReq = null;
						
						if (axExt instanceof FetchRequest) {
							fetchReq = (FetchRequest) axExt;
							required = fetchReq.getAttributes(true);
							optional = fetchReq.getAttributes(false);
						}
						if (SRegExt instanceof SRegRequest) {
							sregReq = (SRegRequest) SRegExt;
							SRegRequired = sregReq.getAttributes(true);
							SRegOptional = sregReq.getAttributes(false);
							System.err.println("SREG request found:" + sregReq);
						}
						Map<String, String> userDataExt = new HashMap<String, String>();
						
						// TODO: provide mapping between AX urls and our
						// LDAP elements
						
						Map<String,String> map = new HashMap<String,String>();
						map.put("http://axschema.org/contact/email", "cn|@"+domain);
						map.put("http://axschema.org/namePerson/friendly", "givenName");
						map.put("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname", "givenName");
						map.put("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", "sn");
						map.put("http://axschema.org/namePerson", "givenName| |sn");
						
						for (Entry<String, String> item : required.entrySet()) {
							if (map.containsKey(item.getValue())){
								String[] lookupKey = map.get(item.getValue()).split("\\|");
								String res = "";
								for (String elem : lookupKey){
									if (entry.getAttribute(elem) != null){
										res += entry.getAttribute(elem).getStringValue();
									} else {
										res += elem;
									}
								}
								System.err.println("Adding required AX:"
										+ item.getValue()+ " -> "+res);
								userDataExt.put(item.getKey(), res);
							} else {
								//TODO: throw error, missing required element!
							}
							
						}
						for (Entry<String, String> item : optional.entrySet()) {
							if (map.containsKey(item.getValue())){
								String[] lookupKey = map.get(item.getValue()).split("\\|");
								String res = "";
								for (String elem : lookupKey){
									if (entry.getAttribute(elem) != null){
										res += entry.getAttribute(elem).getStringValue();
									} else {
										res += elem;
									}
								}
								System.err.println("Adding optional AX:"
									+ item.getValue()+ " -> "+res);
								userDataExt.put(item.getKey(), res);
							}
						}
						for (String elem : SRegRequired) {
							System.err.println("SREG required:" + elem);
							if (entry.getAttribute(elem) != null){
								String res = entry.getAttribute(elem).getStringValue();
								System.err.println("Adding required SREG:"
									+ elem+ " -> "+res);
								userDataExt.put(elem, res);
							} else {
								//TODO:throw error, missing required SREG element
							}
						}
						for (String elem : SRegOptional) {
							System.err.println("SREG optional:" + elem);
							if (entry.getAttribute(elem) != null){
								String res = entry.getAttribute(elem).getStringValue();
								System.err.println("Adding optional SREG:"
									+ elem+ " -> "+res);
								userDataExt.put(elem, res);
							}
						}
						if (fetchReq != null) {
							FetchResponse fetchResp = FetchResponse
									.createFetchResponse(fetchReq, userDataExt);
							response.addExtension(fetchResp);
						}
						if (sregReq != null) {
							SRegResponse sregResp = SRegResponse
									.createSRegResponse(sregReq, userDataExt);
							response.addExtension(sregResp);
						}
						
					}
					System.err
							.println("Came quite far:" + userSelectedId + " - "
									+ userSelectedClaimedId + " - " + values[0]);
				}
			} catch (MessageException e1) {
				throw new ServletException(e1);
			}
			try {
				manager.sign((AuthSuccess) response);
			} catch (ServerException e) {
				throw new ServletException(e);
			} catch (AssociationException e) {
				throw new ServletException(e);
			}
			
			session.setAttribute("isAuthenticated", isAuthenticated);
			resp.sendRedirect(response.getDestinationUrl(true));
			
		} else if ("check_authentication".equals(mode)) {
			// --- processing a verification request ---
			response = manager.verify(params);
			responseText = response.keyValueFormEncoding();
		} else {
			// --- error response ---
			response = DirectError.createDirectError("Unknown request");
			responseText = response.keyValueFormEncoding();
		}
		
		// return the result to the user
		directResponse(resp, responseText);
	}
	
	private String writeXRDS(String domain) {
		String SERVER_ENDPOINT = "http://" + domain + ":8080" + SERVER_URI;
		StringBuilder result = new StringBuilder();
		result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		result.append("<xrds:XRDS xmlns:xrds=\"xri://$xrds\" xmlns:openid=\"http://openid.net/xmlns/1.0\"");
		result.append(" xmlns=\"xri://$xrd*($v*2.0)\">");
		result.append("<XRD version=\"2.0\"><Service priority=\"10\">");
		result.append("<Type>http://specs.openid.net/auth/2.0/signon</Type>");
		result.append("<Type>http://openid.net/sreg/1.0</Type>");
		result.append("<Type>http://openid.net/extensions/sreg/1.1</Type>");
		result.append("<Type>http://schemas.openid.net/pape/policies/2007/06/phishing-resistant</Type>");
		result.append("<Type>http://openid.net/srv/ax/1.0</Type>");
		result.append("<URI>" + SERVER_ENDPOINT + "</URI>");
		result.append("</Service></XRD>");
		result.append("</xrds:XRDS>");
		return result.toString();
	}
	
	private String directResponse(HttpServletResponse httpResp, String response)
			throws IOException {
		ServletOutputStream os = httpResp.getOutputStream();
		os.write(response.getBytes());
		os.close();
		
		return null;
	}
}
