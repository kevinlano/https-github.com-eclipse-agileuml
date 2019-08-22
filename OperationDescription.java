import java.util.Vector;
import java.util.List;
import java.util.ArrayList; 
import java.io.*;

/* Package: EIS */ 
/******************************
* Copyright (c) 2003,2019 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/

public class OperationDescription extends BehaviouralFeature
{ private Vector maintainOps = new Vector(); // extra ops for Dbi, in case of set


  public OperationDescription(String nme,
                              Entity e, String op, String role)
  { // E is the entity being operated on, op the 
    // kind of op: create, delete, add, etc
    super(nme,new Vector(),true,null);
    setEntity(e);
    setStereotypes(new Vector()); 
    stereotypes.add(op);
    if (role != null && !(role.equals("")))
    { stereotypes.add(role); } 

    System.out.println("Action: " + op + " Stereotypes: " + stereotypes); 
      
    if (op.equals("create"))
    { setParameters(e.getAttributes()); }
    if (op.equals("searchBy"))
    { Attribute att = e.getAttribute(role); 
      if (att != null)
      { Vector pars = new Vector(); 
        pars.add(att); 
        setParameters(pars);
      } 
    }
    else if (op.equals("set"))
    { Vector pars = new Vector(); 
      Attribute att = e.getAttribute(role); 
      if (att != null)
      { Vector uniq = e.getUniqueAttributes();
        if (uniq.contains(att))
        { System.err.println("Cannot define set on key " + att); } 
        else 
        { pars.add(att);
          pars.addAll(uniq);   // put at end because of SQL UPDATE
          setParameters(pars); 
        }
      }
    }         
    else if (op.equals("edit"))
    { Vector epars = e.getAttributes(); 
      Vector pars = new Vector(); 
      Vector uniq = e.getUniqueAttributes(); 
      pars.addAll(epars); 
      pars.removeAll(uniq); 
      pars.addAll(uniq); // so they are at end -- but why???
      setParameters(pars); 
    }
    else if (op.equals("list")) // no parameters
    { } 
    else if (op.equals("get") || op.equals("delete")) // delete, get
    { Vector keys = e.getUniqueAttributes();
      if (keys.size() == 0)
      { System.err.println("Cannot define operation: no primary key"); 
        return; 
      }
      else 
      { setParameters(keys); } 
    }
    else if (op.equals("add"))
    { Association ast = entity.getRole(role); 
      if (ast == null) 
      { System.err.println("Error: not a valid role: " + role); 
        return; 
      }
      else 
      { Entity entity2 = ast.getEntity2(); 
        Vector bkeys = entity2.getUniqueAttributes(); 
        Vector pars = new Vector(); 
        pars.addAll(e.getUniqueAttributes()); 
        pars.addAll(bkeys); 
        setParameters(pars); 
      }
    }
    else if (op.equals("remove"))
    { Association ast = entity.getRole(role); 
      if (ast == null) 
      { System.err.println("Error: not a valid role: " + role); 
        return; 
      }
      else 
      { Entity entity2 = ast.getEntity2(); 
        Vector bkeys = entity2.getUniqueAttributes(); 
        Vector pars = new Vector(); 
        pars.addAll(bkeys); 
        setParameters(pars); 
      }
    }
  } // add: e's key and entity2 key. remove: entity2 key. check e key + some atts

  public void addDbiMaintainOps(Vector ops)
  { maintainOps.addAll(ops); } 

  public String getMaintainOps()
  { String res = ""; 
    for (int i = 0; i < maintainOps.size(); i++) 
    { res = res + maintainOps.get(i); }
    return res; 
  }  

  public String getAction()
  { if (stereotypes.size() > 0)
    { return (String) stereotypes.get(0); } 
    return ""; 
  } 

  public void saveData(PrintWriter out)
  { String nme = getName();
    String ename = getEntityName();
    String stereos = ""; 
    for (int p = 0; p < stereotypes.size(); p++) 
    { stereos = stereos + " " + stereotypes.get(p); }
    out.println("UseCase:");
    out.println(nme + " " + ename + " " + stereos);  // op + role
    out.println(); 
  }

  public void saveModelData(PrintWriter out, Vector saved)
  { String nme = getName();
    String ename = getEntityName();
    String stereos = ""; 


    out.println(nme + " : OperationDescription");
    out.println(nme + ".name = \"" + nme + "\""); 
    out.println(nme + ".owner = " + ename); 

    for (int p = 0; p < stereotypes.size(); p++) 
    { out.println("\"" + stereotypes.get(p) + "\" : " + nme + ".stereotypes"); }
 
    out.println(); 
  }

  public String getDbiParameterDec()
  { String res = ""; 
    Vector pars = getParameters(); 
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i); 
      String attnme = att.getName(); 
      Type typ = att.getType(); 
      if (typ.getName().equals("boolean"))
      { res = res + "String " + attnme; } 
      else
      { res = res + typ.getJava() + " " + attnme; } 
      if (i < pars.size() - 1)
      { res = res + ","; } 
    }
    return res; 
  }      

  private String getDbiCallList()
  { String res = "";
    Vector pars = getParameters();
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i);
      Type t = att.getType();
      String nme = att.getName();
      if ("int".equals(t.getName()) || "long".equals(t.getName()))
      { nme = "i" + nme; }
      else if ("double".equals(t.getName()))
      { nme = "d" + nme; }
      res = res + nme;
      if (i < pars.size() - 1)
      { res = res + ", "; }
    }
    return res;
  }
 
  public String getDbiOp()
  { // Op is: dbi.action + ename(pars);
    String pars = getDbiCallList();
    return "dbi." + getODName() + "(" + pars + ");";
  } // plus the role name in case of get/add/remove

  public String getODName()
  { String action = getStereotype(0);
    if (action.equals("query"))
    { return getODName(1); } 

    String ename = entity.getName();
    if (action.equals("get") || action.equals("add") || action.equals("remove") ||
        action.equals("searchBy") || action.equals("set"))
    { return action + ename + getStereotype(1); } 
    else 
    { return action + ename; } 
  }

  public String getODName(int i)
  { String action = getStereotype(i);
    if (action.equals("query"))
    { return getODName(i+1); } 

    String ename = entity.getName();
    if (action.equals("get") || action.equals("add") || action.equals("remove") ||
        action.equals("searchBy") || action.equals("set"))
    { return action + ename + getStereotype(i+1); } 
    else 
    { return action + ename; } 
  }

  public SQLStatement getSQL0()
  { String action = getStereotype(0);
    String ename = entity.getName();
    ArrayList ents = new ArrayList();
    ents.add(ename);
    ArrayList flds = new ArrayList();
    Vector pars = getParameters();
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i);
      flds.add(att.getName());
    }

    if (action.equals("create"))
    { return 
        new SQLStatement("INSERT",ents,flds,
                         new ArrayList(),"");
    }
    else if (action.equals("delete"))
    { return 
        new SQLStatement("DELETE",ents,flds,
                         new ArrayList(),"");
    }
    else if (action.equals("edit") || action.equals("set"))
    { Vector keys = entity.getUniqueAttributes();
      Vector pars2 = (Vector) pars.clone();
      pars2.removeAll(keys);
      String wre = SQLStatement.buildWhere0(ModelElement.getNames(keys));
      return 
        new SQLStatement("UPDATE",ents,ModelElement.getNames(pars2),
                         new ArrayList(),wre);
    }
    else if (action.equals("get"))
    { String role = getStereotype(1); 
      Association ast = entity.getRole(role); 
      if (ast == null) 
      { System.err.println("No role named " + role + " for entity " + entity); 
        return null; 
      } 
      else 
      { Entity entity2 = ast.getEntity2(); 
        Vector bfeatures = ModelElement.getNames(entity2.getAttributes()); 
        Vector akeys = entity.getUniqueAttributes();      
        Attribute key = (Attribute) akeys.get(0);
        String akey0 = key.getName(); 
         
        String akey = ename + "." + akey0;   // A.akey
        ArrayList ents2 = new ArrayList(); 
        ents2.add(ename); 
        ents2.add(entity2.getName()); 
        ArrayList keys2 = new ArrayList(); 
        keys2.add(akey); 
        String wre2 = SQLStatement.buildWhere0(keys2);  // akey = ?
        Vector cons = ast.getConstraints(); 
        if (cons.size() > 0 && ast.isManyMany())
        { wre2 = ((Constraint) cons.get(0)).toSQL() + " AND " + wre2; }
        // AND of constraints
        else if (ast.getCard2() == MANY) // foreign key at entity2 end
        { String bkey = entity2.getName() + "." + akey0;
          bfeatures.remove(akey0); 
          bfeatures.add(bkey);
          wre2 = wre2 + " AND " + bkey + " = " + akey; 
        }
        else // foreign key at entity1 end
        { Vector bkeys = entity2.getUniqueAttributes(); 
          Attribute bkeyatt = (Attribute) bkeys.get(0);
          String bkeyname = bkeyatt.getName();  
          bfeatures.remove(bkeyname); 
          String bkey = entity2.getName() + "." + bkeyname; 
          bfeatures.add(bkeyname); 
          wre2 = wre2 + " AND " + bkey + " = " + ename + "." + bkeyname; 
        } 
        return new SQLStatement("SELECT",ents2,bfeatures,new ArrayList(), 
                                wre2); 
      } 
    } 
    else if (action.equals("list"))
    { Vector star = entity.getAttributes();  
      return new SQLStatement("SELECT",ents,ModelElement.getNames(star),
                              new ArrayList(),null); 
    } 
    else if (action.equals("searchBy"))
    { Vector star = entity.getAttributes(); 
      SQLStatement stat = new SQLStatement("SELECT",ents,ModelElement.getNames(star),
                              new ArrayList(),null);
      Vector atts = new Vector(); 
      atts.add(getStereotype(1)); 
      stat.buildWhere(atts); 
      return stat; 
    } 
    else if (action.equals("add"))
    { String role = getStereotype(1); 
      Association ast = entity.getRole(role); 
      if (ast == null) 
      { System.err.println("No role named " + role + " for entity " + entity); 
        return null; 
      } 
      else 
      { Entity entity2 = ast.getEntity2(); 
        Vector bkeys = ModelElement.getNames(entity2.getUniqueAttributes());
        String bkey = (String) bkeys.get(0); 
        String akey = (String) ((Attribute) getParameters().get(0)).getName(); 
        bkey = entity2.getName() + "." + bkey; 
        akey = entity2.getName() + "." + akey; 
        ArrayList ents2 = new ArrayList(); 
        ents2.add(entity2.getName()); 
        ArrayList forkeys = new ArrayList(); 
        forkeys.add(akey); 
        ArrayList localkeys = new ArrayList(); 
        localkeys.add(bkey); 
        String wre = SQLStatement.buildWhere0(localkeys); 
        return new SQLStatement("UPDATE",ents2,forkeys,new ArrayList(),wre); 
      }
    } 
    else if (action.equals("remove"))
    { String role = getStereotype(1); 
      Association ast = entity.getRole(role); 
      if (ast == null) 
      { System.err.println("No role named " + role + " for entity " + entity); 
        return null; 
      } 
      else  // assume it is ONE-MANY
      { Entity entity2 = ast.getEntity2(); 
        Vector bkeys = ModelElement.getNames(entity2.getUniqueAttributes());
        String bkey = (String) bkeys.get(0); 
        String akey = (String) ((Attribute) getParameters().get(0)).getName(); 
        bkey = entity2.getName() + "." + bkey; 
        // akey = entity2.getName() + "." + akey; 
        ArrayList ents2 = new ArrayList(); 
        ents2.add(entity2.getName()); 
        ArrayList localkeys = new ArrayList(); 
        // localkeys.add(akey); 
        localkeys.add(bkey); 
        // String wre = SQLStatement.buildWhere0(localkeys); 
        return new SQLStatement("DELETE",ents2,localkeys,new ArrayList(),""); 
      }
    } 
    System.out.println("Unknown action: " + action); 
    return null;
  }  
 
  public String getServletCode()
  { String res = "";
    String sname = getODName();
    String dbiop = getDbiOp();
    String action = getStereotype(0); 

    String ename = entity.getName(); 
    res = "import java.io.*;\n\r" +
          "import java.util.*;\n\r" +
          "import javax.servlet.http.*;\n\r" +
          "import javax.servlet.*;\n\r";
    if (action.equals("list") || action.equals("check") ||
        action.equals("get") || action.equals("searchBy")) 
    { res = res + "import java.sql.*;\n\r"; }

    res = res + 
      "public class " + sname +
      "Servlet extends HttpServlet\n\r"; 
    res = res + "{ private Dbi dbi; \n\r\n\r" +
          "  public " + sname + "Servlet() {}\n\r\n\r";

    res = res + "  public void init(ServletConfig cfg)\n\r" +
          "  throws ServletException\n\r" +
          "  { super.init(cfg);\n\r" +  
          "    dbi = new Dbi();\n\r" + 
          "  }\n\r\n\r"; 
    res = res + 
      "  public void doGet(HttpServletRequest req,\n" +
      "              HttpServletResponse res)\n" + 
      "  throws ServletException, IOException\n" +
      "  { res.setContentType(\"text/html\");\n" +
      "    PrintWriter pw = res.getWriter();\n" +
      "    ErrorPage errorPage = new ErrorPage();\n";

    Vector pars = getParameters();
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i);
      String extractatt = att.extractCode();
      String testatt = att.getServletCheckCode();
      res = res + extractatt + testatt;
    }

    if (action.equals("create") || action.equals("edit") || action.equals("set"))
    { Vector tests = entity.getInvariantCheckTests(pars);
      System.out.println("Entity inv checks: " + tests);
      for (int k = 0; k < tests.size(); k++) 
      { String test = (String) tests.get(k); 
        res = res + 
          "    if (" + test + ") { }\n" + 
          "    else \n" + 
          "    { errorPage.addMessage(\"Constraint : " + test + " failed\"); }\n";
      }
    }   // for setatt, generate update code

    String code; 
    if (action.equals("create") || action.equals("delete") ||
        action.equals("add") || action.equals("set") ||
        action.equals("remove") || action.equals("edit"))
    { code = dbiop + "\n" +
      "      CommandPage cp = new CommandPage();\n" +
      "      pw.println(cp);\n"; 
    } 
    else // get, list, check   
    { if (action.equals("get"))
      { String role = getStereotype(1); 
        Association ast = entity.getRole(role); 
        if (ast == null) { return res; } 
        Entity entity2 = ast.getEntity2();
        ename = entity2.getName(); 
      } 
      String resultPage = ename.toLowerCase() + "resultpage"; 
      code = "ResultSet resultSet = " + dbiop + "\n" + 
      "       " + ename + "ResultPage " + resultPage + 
                                  " = new " + ename + "ResultPage();\n" + 
      "       while (resultSet.next())\n" + 
      "       { " + resultPage + ".addRow(resultSet); }\n" + 
      "       pw.println(" + resultPage + ");\n" + 
      "       resultSet.close();\n"; 
    }
    res = res + 
      "    if (errorPage.hasError())\n" +
      "    { pw.println(errorPage); }\n" +
      "    else \n" +
      "    try { " + code + 
      "    } catch (Exception e) \n" + 
      "    { e.printStackTrace(); \n" + 
      "      errorPage.addMessage(\"Database error\"); \n" + 
      "      pw.println(errorPage); }\n" + 
      "    pw.close();\n" + 
      "  }\n\n";

    res = res +
      "  public void doPost(HttpServletRequest req,\n" + 
      "               HttpServletResponse res)\n" +
      "  throws ServletException, IOException\n" +
      "  { doGet(req,res); }\n\n"; 

    res = res + 
      "  public void destroy()\n" +
      "  { dbi.logoff(); }\n" +
      "}\n";
    return res;
  }

  public String jspUpdateDeclarations(String ename)
  { String bean = ename.toLowerCase();
    String beanclass = "beans." + ename + "Bean";
    return "<jsp:useBean id=\"" + bean +
           "\" scope=\"session\" \n " + 
           "class=\"" + beanclass + "\"/>";
  }

  public String jspParamTransfers(String ename, Vector atts)
  { String bean = ename.toLowerCase();
    String res = "";
    for (int i = 0; i < atts.size(); i++)
    { Attribute att = (Attribute) atts.get(i);
      String nme = att.getName();
      res = res +
        "<jsp:setProperty name=\"" + bean +
        "\"  property=\"" + nme + 
        "\"  param=\"" + nme + "\"/>\n\r";
    }
    return res;
  }

  
  public String jspUpdateText(String op,
    String ename, Vector atts)
  { String bean = ename.toLowerCase();
         String dec = jspUpdateDeclarations(ename);
    String sets = jspParamTransfers(ename, atts);
    String res = dec + "\n\r" + sets + "\n\r" +
      "<html>\n\r" +
      "<head><title>" + op + "</title></head>\n\r" +
      "<body>\n\r" +
      "<h1>" + op + "</h1>\n\r" +
      "<% if (" + bean + ".is" + op + "error())\n\r" +
      "{ %> <h2>Error in data: <%= " + bean +
      ".errors() %></h2>\n\r" +
      "<h2>Press Back to re-enter</h2> <% }\n\r" +
      "else { " + bean + "." + op + "(); %>\n\r" +
      "<h2>" + op + " performed</h2>\n\r" +
      "<% } %>\n\r\n\r" +
      "<hr>\n\r\n\r" +
      "<%@ include file=\"commands.html\" %>\n\r" +
      "</body>\n\r</html>\n\r";
    return res;
  }

  public String jspQueryDeclarations(String ename)
  { String bean = ename.toLowerCase();
    String beanclass = "beans." + ename + "Bean";
    String res = "<%@ page import = \"java.util.*\" %>\n\r" +
      "<%@ page import = \"beans.*\" %>\n\r" +
      "<jsp:useBean id=\"" + bean +
           "\" scope=\"session\" \n\r " + 
           "class=\"" + beanclass + "\"/>";
      return res;
  }

  public String jspQueryText(String op,
                             String ename, Vector atts, Entity ent)
  { String bean = ename.toLowerCase();
    String dec = jspQueryDeclarations(ename);
    String sets = jspParamTransfers(ename, atts);
    Entity ent2 = ent; 
    String action = getStereotype(0); 
    if (action.equals("get"))
    { String role = getStereotype(1); 
      Association ast = ent.getRole(role); 
      if (ast != null)
      { ent2 = ast.getEntity2(); }
    }
    String e2name = ent2.getName(); 
    String e2bean = e2name.toLowerCase(); 

    String res = dec + "\n\r" + sets + "\n\r" +
      "<html>\n\r" +
      "<head><title>" + op + " results</title></head>\n\r" +
      "<body>\n\r" +
      "<h1>" + op + " results</h1>\n\r" +
      "<% Iterator " + bean + "s = " + bean + "." + op +
      "(); %>\n\r" +
      "<table border=\"1\">\n\r" +
      ent2.getTableHeader() + "\n\r" +
      "<% while (" + bean + "s.hasNext())\n\r" +
      "{ " + e2name + "VO " + e2bean + "VO = (" + 
      e2name + "VO) " + bean + "s.next(); %>\n\r" +
      ent2.getTableRow() + "\n\r" +
      "<% } %>\n\r</table>\n\r\n\r<hr>\n\r\n\r" +
      "<%@ include file=\"commands.html\" %>\n\r" +
      "</body>\n\r</html>\n\r";
    return res;
  }

  public String getJsp()
  { String action = getODName();
    String op = getStereotype(0);
    String ename = entity.getName();
    Vector pars = getParameters();
    if (op.equals("create") || op.equals("delete") ||
        op.equals("edit") || op.equals("add") ||
        op.equals("set") || op.equals("remove"))
    { return jspUpdateText(action,ename,pars); }
    return jspQueryText(action,ename,pars,entity);
  }

  public String getInputPage()
  { String codebase = "http://127.0.0.1:8080/servlets/";
    String op = getODName();
    String action = getStereotype(0);
    String jsp = codebase + op + ".jsp";
    String method = "GET";
    if (action.equals("create") || action.equals("delete") ||
        action.equals("edit") || action.equals("add") || action.equals("set") ||
        action.equals("remove"))
    { method = "POST"; }
    String res = "<html>\n\r" +
      "<head><title>" + op + " form</title></head>\n\r" +
      "<body>\n\r" +
      "<h1>" + op + " form</h1>\n\r" +
      "<form action = \"" + jsp + "\" method = \"" +
      method + "\" >\n\r";
    Vector pars = getParameters();
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i);
      res = res + att.getFormInput() + "\n\r";
    }
    res = res + "<input type=\"submit\" value = \"" + 
          op + "\"/>\n\r</form>\n\r</body>\n\r</html>";
    return res;
  }

  public String getGenerationClass()
  { String nme = getName();
    String codebase = "http://localhost:8080/servlet/"; 
    String ename = entity.getName();
    String action = getStereotype(0);
    String rolename = "";
    if (getStereotype(1) != null)
    { rolename = getStereotype(1); } 
    String servlet = codebase + getODName() + "Servlet";
    String res = "public class " + nme + 
      "Page extends BasePage\n" +
      "{ protected HtmlForm form = new HtmlForm();\n" +
      "  protected HtmlInput button = new HtmlInput();\n\n" +
      "  public " + nme + "Page()\n" +
      "  { super();\n" +
      "    HtmlText heading = new HtmlText(\"" +
      action + " " + ename + rolename + " form\",\"h1\");\n" +
      "    body.add(0,heading);\n" +
      "    form.setAttribute(\"action\",\"" + 
      servlet + "\");\n" + 
      "    HtmlItem para = new HtmlItem(\"p\");\n" + 
      "    form.setAttribute(\"method\",\"POST\");\n" +
      "    button.setAttribute(\"type\",\"submit\");\n" +
      "    button.setAttribute(\"value\",\"" + 
      action + "\");\n" +
      "    body.add(form);\n";
    Vector pars = getParameters();
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i);
      String attItem = att.getHtmlGen();
      res = res + attItem;
    }
    res = res + "    form.add(button);\n" +
          "  }\n" + "}\n";
    return res;
  } // use GET for query ops

  public String getDbiOpCode()
  { String action = getStereotype(0);
    String ename = entity.getName();
    Vector pars = getParameters();
    String stat = getODName() + "Statement";
    String res = "{ try\n" +
      "    { ";
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i);
      Type t = att.getType();
      String nme = att.getName();
      if ("int".equals(t.getName()) || "long".equals(t.getName()))
      { res = res + "  " + stat + ".setInt(" + (i+1) + ", " +
                            nme + ");\n    "; 
      }
      else if ("double".equals(t.getName()))
      { res = res + "  " + stat + ".setDouble(" + (i+1) + ", " +
                               nme + ");\n    "; 
      }
      else 
      { res = res + "  " + stat + ".setString(" + (i+1) + ", " +
                               nme + ");\n    "; 
      }
    }
    if (action.equals("get") || action.equals("list") || action.equals("check") ||
        action.equals("searchBy"))
    { res = res + "  return " + stat + ".executeQuery();\n" +
            "  } catch (Exception e) { e.printStackTrace(); }\n" +
            "  return null; }\n";
    }
    else 
    { res = res + "  " + stat + ".executeUpdate();\n" +
        "    connection.commit();\n" +
        "  } catch (Exception e) { e.printStackTrace(); }\n}\n";
    }
    return res;
  }

  public static void createControllerBean(Vector usecases, Vector entities, PrintWriter out)
  { out.println("package beans;\n\r\n\r");
    out.println("import java.util.*;\n\r");
    out.println("\n\r");
    out.println("public class ControllerBean\n\r");
    out.println("{ Controller cont;\n\r");
    out.println("\n\r");
    out.println("  public ControllerBean() { cont = Controller.inst(); }\n\r");
    out.println("\n\r");
    for (int i = 0; i < usecases.size(); i++)
    { Object obj = usecases.get(i);
      if (obj instanceof UseCase)
      { UseCase uc = (UseCase) obj;
        uc.generateControllerBeanAttributes(out);
      } // parameters must all have different names, only one result. 
    }
    out.println("\n\r");
    for (int i = 0; i < usecases.size(); i++)
    { Object obj = usecases.get(i);
      if (obj instanceof UseCase)
      { UseCase uc = (UseCase) obj;
        uc.generateControllerBeanOps(out);
      }
    }
    out.println("}\n\r");
  }


  public static void createWebServiceBean(Vector usecases, Vector entities, PrintWriter out)
  { out.println("import java.util.*;\n\r");
    out.println("import javax.jws.WebService;\n\r");
    out.println("import javax.jws.WebMethod;\n\r");
    out.println("import javax.jws.WebParam;\n\r\n\r");
    out.println("@WebService( name = \"ControllerWebBean\",  serviceName = \"ControllerWebBeanService\" )\n\r");
    out.println("public class ControllerWebBean\n\r");
    out.println("{ Controller cont;\n\r");
    out.println("\n\r");
    out.println("  public ControllerWebBean() { cont = Controller.inst(); }\n\r");
    out.println("\n\r");
    for (int i = 0; i < usecases.size(); i++)
    { Object obj = usecases.get(i);
      if (obj instanceof UseCase)
      { UseCase uc = (UseCase) obj;
        uc.generateWebServiceOp(out);
      }
    }
    out.println("\n\r");
    out.println("}\n\r\n\r");
  }

}
