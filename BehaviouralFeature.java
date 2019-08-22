import java.util.Vector; 
import java.io.*; 
import java.util.StringTokenizer; 
import javax.swing.JOptionPane; 
import java.util.Set; 
import java.util.HashSet; 


/******************************
* Copyright (c) 2003,2019 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/
/* package: Class Diagram
*/

public class BehaviouralFeature extends ModelElement
{ private Vector parameters = new Vector(); // Attribute
  private boolean query = true;
  private Type resultType;
  private Type elementType; // Of the result
  private Expression pre;   // For OCL operation specs
  private Expression bpre;  // For preconditions of B ops
  private Expression post; 
  protected Entity entity;  // the owner of the operation, null for global op
  private UseCase useCase;  // the use case that owns the operation, if any
  private boolean instanceScope = true; 
  private boolean ordered = false;  // true for ops on sequences
  private boolean sorted = false;   // true if op returns a sorted set
  private Statemachine sm = null; 
  private Statement activity = null; 
  private String text = "";  // The text of the operation

  private boolean derived = false; 
  private boolean bx = false; 

  private Vector readfr = null; 
  private Vector writefr = null; 
  private Expression defcond = null; 
  private Expression detcond = null; 


  public BehaviouralFeature(String nme, Vector pars,
                            boolean readstatus,
                            Type res)
  { super(nme);
    parameters = pars;
    query = readstatus;
    if (query) { addStereotype("query"); } 
    resultType = res;
    if (res != null && Type.isCollectionType(res)) { } 
    else 
    { elementType = res; }  // primitive, String, entity type
  }

  public BehaviouralFeature(String nme) 
  { // creates update operation
    super(nme); 
    query = false; 
    resultType = null; 
    elementType = null; 
  } 

  public void setType(Type rt)
  { resultType = rt; } 

  public void setBx(boolean b)
  { bx = b; } 

  public Object clone()
  { BehaviouralFeature res = new BehaviouralFeature(getName(), parameters, query, resultType); 
    res.setElementType(elementType); 
    res.setStatechart(sm); 
    res.setActivity(activity); 
    res.setEntity(entity); 
    res.setInstanceScope(instanceScope); 
    res.setDerived(derived); 
    res.setOrdered(ordered); 
    res.setSorted(sorted); 
    res.setPre(pre);    // I assume these 
    res.setPost(post);  // should be copied, or cloned? 
    res.setBx(bx); 

    return res; 
  } // and copy the use case, readfr, etc? 

  public void addParameter(Attribute att)
  { parameters.add(att); } 

  public void setStatechart(Statemachine s)
  { sm = s; } 

  public void setActivity(Statement st)
  { activity = st; } 

  public void setResultType(Type t)
  { resultType = t; } 

  public Vector getReadFrame()
  { if (readfr != null) { return readfr; }   // to avoid recursion
    readfr = new Vector(); 
    Vector res = new Vector(); 
    // subtract each params name:
    if (post != null) 
    { res.addAll(post.readFrame()); }
    if (activity != null) 
    { res.addAll(activity.readFrame()); } 
  
    for (int p = 0; p < parameters.size(); p++) 
    { String par = "" + parameters.get(p); 
      res.remove(par); 
    } 
    // System.out.println("Invocation " + this + " READ FRAME= " + res); 
    readfr = res; 
    return res; 
  }         

  public Vector getWriteFrame(Vector assocs)
  { if (writefr != null) { return writefr; }   // to avoid recursion
    writefr = new Vector(); 
    Vector res = new Vector(); 
    if (post != null) 
    { res.addAll(post.wr(assocs)); }  
    if (activity != null) 
    { res.addAll(activity.writeFrame()); } 

    // subtract each params name:
    for (int p = 0; p < parameters.size(); p++) 
    { String par = "" + parameters.get(p); 
      res.remove(par); 
    } 

    // System.out.println("Invocation " + this + " WRITE FRAME= " + res); 
    writefr = res; 
    return res; 
  }         

  public Statemachine getSm()
  { return sm; } 

  public Statement getActivity()
  { return activity; } 

  public boolean isClassScope()
  { return !instanceScope; } 

  public boolean isCached() 
  { if (hasStereotype("cached"))
    { return true; } 
    return false; 
  } 

  public void setCached(boolean b)
  { if (b) 
    { addStereotype("cached"); } 
    else 
    { removeStereotype("cached"); } 
  } 

  public void setInstanceScope(boolean b)
  { if (b)
    { removeStereotype("static"); 
      instanceScope = true; 
    } 
    else 
    { addStereotype("static");   // ??
      instanceScope = false;
    } 
  } 

  public void setStatic(boolean b)
  { if (b)
    { addStereotype("static"); 
      instanceScope = false; 
    } 
    else 
    { removeStereotype("static");   // ??
      instanceScope = true;
    } 
  } 

  public void setQuery(boolean q)
  { query = q; 
    if (query) { addStereotype("query"); } 
    else { removeStereotype("query"); } 
  } 

  public void setDerived(boolean d) 
  { derived = d; } 

  public boolean isDerived()
  { return derived; } 

  public BehaviouralFeature mergeOperation(Entity e, BehaviouralFeature eop)
  { // Assume that both are instanceScope

    BehaviouralFeature res = null; 
    Type newt = null; 
    boolean isquery = false; 
    Vector newpars = new Vector();
    Expression newpost = null;
    Expression newpre = null; 

    if (resultType == null) 
    { if (eop.resultType == null) {}
      else 
      { newt = eop.resultType; } 
    } 
    else 
    { if (eop.resultType == null) 
      { newt = resultType; }
      else 
      { newt = resultType.mergeType(eop.resultType);
        if (newt == null) 
        { return null; }
      } 
    } 
    // parameters also must match
    if (parameters.size() > eop.parameters.size())
    { newpars = parameters; } 
    else 
    { parameters = eop.parameters; } 
    // take the union of them. 

    if (query && eop.query)
    { isquery = true; }

    if ((pre + "").equals(eop.pre + ""))
    { newpre = pre; }
    else
    { newpre = Expression.simplify("&",pre,eop.pre,null); }

    if ((post + "").equals(eop.post + ""))
    { newpost = post; }
    else
    { newpost = Expression.simplify("or",post,eop.post,null); } 

    res = 
      new BehaviouralFeature(getName(),newpars,isquery,newt); 
    res.setEntity(e); 
    res.setPre(newpre); 
    res.setPost(newpost); 
    return res; 
  } // also set elementType 

  public void setEntity(Entity ent)
  { entity = ent; }

  public Entity getEntity()
  { return entity; } 

  public void setOwner(Entity ent)
  { entity = ent; }

  public void setUseCase(UseCase uc)
  { useCase = uc; } 

  public UseCase getUseCase()
  { return useCase; } 

  public String getEntityName()
  { if (entity == null) { return ""; } 
    return entity.getName(); 
  } 

  public void setPre(Expression p)
  { pre = p; }

  public void setPost(Expression p)
  { post = p; }

  public void setOrdered(boolean ord)
  { ordered = ord; } 

  public void setSorted(boolean ord)
  { sorted = ord; } 

  public void setText(String line) 
  { text = line; } 

  public void clearText()
  { text = ""; } 

  public boolean hasText()
  { return text != null && text.length() > 0; } 

  public void addBPre(Expression e)
  { if (bpre == null) 
    { bpre = e; } 
    else 
    { bpre = Expression.simplifyAnd(bpre,e); } 
  } 

  public void addPost(Expression e)
  { if (post == null) 
    { post = e; } 
    else 
    { post = Expression.simplifyAnd(post,e); } 
  } 

  public boolean isDefinedinSuperclass()
  { if (entity == null) 
    { return false; } 
    Entity sup = entity.getSuperclass(); 
    if (sup == null) 
    { return false; } 
    BehaviouralFeature sop = sup.getDefinedOperation(name); 
    if (sop == null) 
    { return false; } 
    return true; 
  } 
    
  public boolean isQuery()
  { return query; }  // invariants/postconditions for this op should have 
                     // result in succ as only updated feature

  public boolean isUpdate()
  { return query == false; } 

  public boolean isAbstract()
  { return hasStereotype("abstract"); } 

  public boolean isSequential()
  { return hasStereotype("sequential"); } 

  public boolean isStatic()
  { return hasStereotype("static"); } 

  public boolean isFinal()
  { return hasStereotype("final"); } 

  public boolean isOrdered()
  { return ordered; } 

  public boolean isSorted()
  { return sorted; } 

  public boolean isSingleValued()
  { if (resultType != null && !("".equals(resultType)) && 
        !("void".equals(resultType)))
    { if (Type.isCollectionType(resultType))
      { return false; } 
      return true; 
    } 
    return false; 
  } 

  public Vector getParameters()
  { return parameters; }

  public Attribute getParameter(int i) 
  { return (Attribute) parameters.get(i); } 

  public void setParameters(Vector pars)
  { parameters = pars; } 

  public void setElementType(Type et)
  { elementType = et; } 
  // and set the elementType of the resultType. 

  public Type getResultType()
  { if (resultType == null) { return null; } 
    return (Type) resultType.clone(); 
  }

  public Type getElementType()
  { if (elementType == null) { return null; } 
    return (Type) elementType.clone(); 
  } 

  public Expression getPre() 
  { return pre; } 

  public Expression getPost()
  { return post; } 

  public Expression definedness(Vector arguments) 
  { // pre & (pre => def(post))
    Expression res; 
    if (defcond != null) 
    { return substituteParameters(defcond, arguments); } 
    else if (post != null)  
    { if (pre != null) 
      { defcond = (Expression) pre.clone();  
        Expression defpost = post.definedness(); 
        Expression imp = new BinaryExpression("=>", defcond, defpost); 
        imp.setBrackets(true); 
        res = new BinaryExpression("&", defcond, imp);
      } 
      else 
      { defcond = new BasicExpression(true); 
        res = post.definedness();  
      }
    }   
    else 
    { res = new BasicExpression(true); }
    defcond = res; 
 
    // JOptionPane.showMessageDialog(null, "Definedness obligation for " + getName() + " is:\n" + res,
    //   "Internal consistency condition", JOptionPane.INFORMATION_MESSAGE); 
    System.out.println("***> Definedness obligation for " + getName() + " is:\n" + res); 
    return substituteParameters(res, arguments);  
  } 

  public Expression determinate(Vector arguments) 
  { // (pre => det(post))
    if (detcond != null) 
    { return substituteParameters(detcond, arguments); } 
    else 
    { detcond = new BasicExpression(true); 
      Expression imp = null; 
      if (post != null) 
      { Expression detpost = post.determinate(); 
        if (pre != null) 
        { imp = new BinaryExpression("=>", pre, detpost); 
          imp.setBrackets(true);
        } 
        else 
        { imp = detpost; } 
      } 
      else 
      { imp = new BasicExpression(true); } 
 
      // JOptionPane.showMessageDialog(null, "Determinacy obligation for " + getName() + " is:\n" + imp, 
      // "Internal consistency condition", JOptionPane.INFORMATION_MESSAGE); 
      System.out.println("***> Determinacy obligation for " + getName() + " is:\n" + imp); 
      detcond = imp; 
      return substituteParameters(imp, arguments);
    }  
  } 

  public boolean parametersMatch(Vector pars)
  { if (pars.size() == parameters.size()) 
    { return true; } 
    return false; 
  } // and check the types

  public boolean parameterMatch(Vector pars)
  { if (pars.size() != parameters.size()) 
    { return false; } 
    for (int i = 0; i < parameters.size(); i++) 
    { Attribute par = (Attribute) parameters.get(i); 
      Expression arg = (Expression) pars.get(i); 
      if (Type.isSubType(arg.getType(), par.getType())) { }  // = or a subtype
      else 
      { return false; } 
    } 

    return false; 
  } // and check the types

  public Expression substituteParameters(Expression e, Vector arguments) 
  { if (e == null) 
    { return new BasicExpression(true); } 
    Expression res = (Expression) e.clone(); 
    for (int i = 0; i < parameters.size(); i++) 
    { if (i < arguments.size()) 
      { Expression arg = (Expression) arguments.get(i); 
        Attribute par = (Attribute) parameters.get(i); 
        res = res.substituteEq(par.getName(), arg); 
      } 
    } 
    return res; 
  } 

  public Expression substituteParametersPre(Vector arguments) 
  { if (pre == null) 
    { return new BasicExpression(true); } 
    Expression res = (Expression) pre.clone(); 
    for (int i = 0; i < parameters.size(); i++) 
    { if (i < arguments.size()) 
      { Expression arg = (Expression) arguments.get(i); 
        Attribute par = (Attribute) parameters.get(i); 
        res = res.substituteEq(par.getName(), arg); 
      } 
    } 
    return res; 
  } 

  public Expression substituteParametersPost(Vector arguments) 
  { if (post == null) 
    { return new BasicExpression(true); } 
    Expression res = (Expression) post.clone(); 
    for (int i = 0; i < parameters.size(); i++) 
    { if (i < arguments.size()) 
      { Expression arg = (Expression) arguments.get(i); 
        Attribute par = (Attribute) parameters.get(i); 
        res = res.substituteEq(par.getName(), arg); 
      } 
    } 
    return res; 
  } 

  public boolean typeCheck(Vector types, Vector entities)
  { // System.err.println("ERRROR -- calling wrong type-check for " + name); 

    Vector contexts = new Vector(); 
    if (entity != null && instanceScope) 
    { contexts.add(entity); } 
    Vector env = new Vector(); 
    env.addAll(parameters); 
    if (pre != null) 
    { pre.typeCheck(types,entities,contexts,env); } 
    boolean res = false; 
    if (post != null) 
    { res = post.typeCheck(types,entities,contexts,env); } 
    if (activity != null) 
    { res = activity.typeCheck(types,entities,contexts,env); } 
    return res;  
  } // and the activity? 
  // could deduce type and element type of result. 

  public boolean typeCheck(Vector types, Vector entities, Vector contexts, Vector env)
  { Vector contexts1 = new Vector(); 
    if (entity != null && instanceScope) 
    { if (contexts.contains(entity)) { } 
      else 
      { contexts1.add(entity); }
    } 
    contexts1.addAll(contexts); 
 
    Vector env1 = new Vector(); 
    env1.addAll(parameters);
    env1.addAll(env); 

    // System.err.println("Type-check for " + name + " WITH " + env1 + " " + contexts1); 

 
    if (pre != null) 
    { pre.typeCheck(types,entities,contexts1,env1); } 
    boolean res = false; 
    if (post != null) 
    { res = post.typeCheck(types,entities,contexts1,env1); } 
    if (activity != null) 
    { res = activity.typeCheck(types,entities,contexts1,env1); } 
    return res;  
  } // and the activity? 
  // could deduce type and element type of result. 

  public static Vector reconstructParameters(String pars, 
                                             Vector types, Vector entities)
  { Vector parvals = new Vector(); 
    Vector res = new Vector(); 
    StringTokenizer st = new StringTokenizer(pars," :,"); 
    while (st.hasMoreTokens())
    { parvals.add(st.nextToken()); } 
    
    for (int i = 0; i < parvals.size() - 1; i = i + 2) 
    { String var = (String) parvals.get(i); 
      String typ = (String) parvals.get(i+1);     // could be Set(T), etc

      Type elemType = null; 

      if (typ != null) 
      { int bind = typ.indexOf("("); 
        if (bind > 0)  // a set or sequence type 
        { String tparam = typ.substring(bind+1,typ.length() - 1); 
          // typ = typ.substring(0,bind); 
          // Entity eee = (Entity) ModelElement.lookupByName(tparam,entities);
          // if (eee == null) 
          // { System.out.println("Error: invalid element type for operation: " + tparam); }
          // else 
          // { elemType = new Type(eee); }
          elemType = Type.getTypeFor(tparam,types,entities);           
        } 
      }      

      Type t = Type.getTypeFor(typ,types,entities);  

      if (t == null) // must be standard type or an entity
      { System.err.println("ERROR: Invalid type name: " + typ);
        JOptionPane.showMessageDialog(null, "ERROR: Invalid type for parameter: " + typ,
                                      "Type error", JOptionPane.ERROR_MESSAGE); 
      }
      else 
      { Attribute att = new Attribute(var,t,ModelElement.INTERNAL); 
        att.setElementType(elemType); 
        t.setElementType(elemType); 
        res.add(att); 
        System.out.println("Parameter " + var + " type " + t); 
      } 
    }
    return res; 
  }

  public String getParList()
  { String res = ""; 
    for (int i = 0; i < parameters.size(); i++) 
    { Attribute att = (Attribute) parameters.get(i); 
      res = res + att.getName() + " " + att.getType() + " "; 
    }
    return res; 
  }   

  public String getSignature()
  { String res = getName() + "(";
    for (int i = 0; i < parameters.size(); i++) 
    { Attribute att = (Attribute) parameters.get(i); 
      res = res + att.getName() + ": " + att.getType(); 
      if (i < parameters.size() - 1) 
      { res = res + ","; } 
    }
    return res + ")"; 
  }   
    
  public Vector getParameterNames()
  { Vector res = new Vector();
    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i);
      res.add(att.getName());
    }
    return res;
  }

  public String getJavaParameterDec()
  { String res = ""; 
    // System.out.println("PARAMETERS = " + parameters); 

    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i); 
      String attnme = att.getName(); 
      Type typ = att.getType(); 
      if (typ == null) 
      { System.err.println("ERROR: null type for parameter " + att); 
        JOptionPane.showMessageDialog(null, "ERROR: Invalid type for parameter: " + att,
                                      "Type error", JOptionPane.ERROR_MESSAGE); 
        typ = new Type("void",null); 
      } 
      res = res + typ.getJava() + " " + attnme; 
      if (i < parameters.size() - 1)
      { res = res + ","; } 
    }
    return res; 
  }      

  public String getJava6ParameterDec()
  { String res = ""; 
    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i); 
      String attnme = att.getName(); 
      Type typ = att.getType(); 
      if (typ == null) 
      { System.err.println("ERROR: null type for parameter " + att); 
        JOptionPane.showMessageDialog(null, "ERROR: Invalid type for parameter: " + att,
                                      "Type error", JOptionPane.ERROR_MESSAGE); 
        typ = new Type("void",null); 
      } 
      res = res + typ.getJava6() + " " + attnme; 
      if (i < parameters.size() - 1)
      { res = res + ","; } 
    }
    return res; 
  }      

  public String getJava7ParameterDec()
  { String res = ""; 
    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i); 
      String attnme = att.getName(); 
      Type typ = att.getType(); 
      if (typ == null) 
      { System.err.println("ERROR: null type for parameter " + att); 
        JOptionPane.showMessageDialog(null, "ERROR: Invalid type for parameter: " + att,
                                      "Type error", JOptionPane.ERROR_MESSAGE); 
        typ = new Type("void",null); 
      } 
      res = res + typ.getJava7(att.getElementType()) + " " + attnme; 
      if (i < parameters.size() - 1)
      { res = res + ","; } 
    }
    return res; 
  }      

  public String getCSharpParameterDec()  
  { String res = ""; 
    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i); 
      String attnme = att.getName(); 
      Type typ = att.getType(); 
      if (typ == null) 
      { System.err.println("ERROR: null type for parameter " + att); 
        JOptionPane.showMessageDialog(null, "ERROR: Invalid type for parameter: " + att,
                                      "Type error", JOptionPane.ERROR_MESSAGE); 
        typ = new Type("void",null); 
      } 
      res = res + typ.getCSharp() + " " + attnme; 
      if (i < parameters.size() - 1)
      { res = res + ","; } 
    }
    return res; 
  }      

  public String getCPPParameterDec()  
  { String res = ""; 
    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i); 
      String attnme = att.getName(); 
      Type typ = att.getType(); 
      if (typ == null) 
      { System.err.println("ERROR: null type for parameter " + att); 
        JOptionPane.showMessageDialog(null, "ERROR: Invalid type for parameter: " + att,
                                      "Type error", JOptionPane.ERROR_MESSAGE); 
        typ = new Type("void",null); 
      } 
      res = res + typ.getCPP(att.getElementType()) + " " + attnme; 
      if (i < parameters.size() - 1)
      { res = res + ","; } 
    }
    return res; 
  }      

  private String parcats()
  { // Assumes all parameters are primitive/strings
    String res = "";
    if (parameters.size() == 1)
    { Attribute p = (Attribute) parameters.get(0);
      return Expression.wrap(p.getType(), p.getName());
    }
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String par1 = par.getName(); 
      if (i < parameters.size() - 1) 
      { res = res + par1 + " + \", \" + "; } 
      else 
      { res = res + par1; } 
    }
    return res;
  } // works for Java, Java6

  private String parcatsCSharp()
  { // Assumes all parameters are primitive/strings
    String res = "";
    if (parameters.size() >= 1)
    { Attribute p = (Attribute) parameters.get(0);
      res = res + p.getName();
    }
    for (int i = 1; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String par1 = par.getName(); 
      res = res + " + \", \" + " + par1;
    }
    return res;
  } 

  public BExpression getBParameterDec()
  { BExpression res = null;
    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i);
      BExpression attbe = new BBasicExpression(att.getName()); 
      BExpression typebe = 
        new BBasicExpression(att.getType().generateB(att.getElementType())); 
      BExpression conj = new BBinaryExpression(":",attbe,typebe); 
      if (res == null) 
      { res = conj; } 
      else 
      { res = new BBinaryExpression("&",res,conj); } 
    }
    return res;
  }

  public BExpression getBCall()
  { // call of query op, assumed instance scope

    String opname = getName();
    if (entity == null) 
    { opname = opname + "_"; } 
    else 
    { opname = opname + "_" + entity.getName(); }
    String res = opname; 
 
    String ex = entity.getName().toLowerCase() + "x"; 
    Vector epars = new Vector(); 
    epars.add(new BBasicExpression(ex)); 

    if (parameters.size() == 0)
    { return new BBasicExpression(opname,epars); } 

    Vector pars = new Vector(); 
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getName();
      pars.add(new BBasicExpression(dec)); 
    } 
    return new BApplyExpression(new BBasicExpression(opname,epars),pars); 
  }      

  public BExpression getBFunctionDec()
  { // opname_E : es --> (parT --> resultT)
    BExpression res = null;
    for (int i = 0; i < parameters.size(); i++)
    { Attribute att = (Attribute) parameters.get(i);
      BExpression typebe = 
        new BBasicExpression(att.getType().generateB(att.getElementType())); 
      if (res == null) 
      { res = typebe; } 
      else 
      { res = new BBinaryExpression("*",res,typebe); } 
    } // what if empty? 
    
    BExpression ftype; 
    if (resultType != null)  // for Set/Sequence need elementType
    { String rT = resultType.generateB(elementType); 
      if (res == null) 
      { ftype = new BBasicExpression(rT); } 
      else 
      { ftype = new BBinaryExpression("-->",res,
                      new BBasicExpression(rT));
        ftype.setBrackets(true); 
      } 
    }
    else 
    { System.err.println("ERROR: Undefined return type of operation " + this); 
      ftype = null; 
      return null; 
    } 

    String opname = getName();
    if (entity == null)   // also if static
    { opname = opname + "_";
      return new BBinaryExpression(":",new BBasicExpression(opname),ftype);
    } 
    else 
    { opname = opname + "_" + entity.getName(); 
      String ename = entity.getName(); 
      String es = ename.toLowerCase() + "s"; 
      BExpression esbe = new BBasicExpression(es); 
      return new BBinaryExpression(":",new BBasicExpression(opname),
                   new BBinaryExpression("-->",esbe,ftype)); 
    }
  }

  private String parameterList()
  { String res = "";
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getName();
      if (i < parameters.size() - 1)
      { dec = dec + ", "; }
      res = res + dec;
    }
    return res;
  }

  
  public String toString()
  { String res = getName() + "(";
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getName();
      if (i < parameters.size() - 1)
      { dec = dec + ", "; }
      res = res + dec;
    }
    res = res + ")";
    return res;
  }

  public String display()   // what about guards?
  { String res = ""; 
    if (isSequential())
    { res = res + "synchronized "; } 
    if (isAbstract())
    { res = res + "abstract "; } 
    if (isFinal())
    { res = res + "final "; } 
    if (instanceScope) { } 
    else 
    { res = res + "static "; } 
    if (isQuery())
    { res = res + "query ";  } 

    res = res + getSignature(); 

    if (resultType != null)
    { res = res + ": " + resultType; } 
    // if ("Set".equals(resultType + "") || "Sequence".equals(resultType + ""))
    // { res = res + "(" + elementType + ")"; } 
    res = res + "\n"; 
    if (pre != null)
    { res = res + "pre: " + pre + "\n"; } 
    if (post != null) 
    { res = res + "post: " + post + "\n"; }
    if (activity != null)
    { res = res + "activity: " + activity + "\n"; }
    if (hasText())
    { res = res + "text:\n"; 
      res = res + text + "\n"; 
    }     
    return res; 
  } 

  public String displayForUC()   // what about guards?
  { String res = ""; 

    res = res + getSignature(); 

    if (resultType != null)
    { res = res + " : " + resultType; } 
    res = res + "\n\r"; 
    if (pre != null)
    { res = res + "pre: " + pre + "\n\r"; }
    else 
    { res = res + "pre: true\n\r"; }
    
    if (post != null) 
    { res = res + "post: " + post + "\n\r"; }
    else 
    { res = res + "post: true\n\r"; }
    return res; 
  } 

  public Vector equivalentsUsedIn() 
  { Vector res = new Vector(); 
    if (pre != null) 
    { res.addAll(pre.equivalentsUsedIn()); } 
    if (post != null) 
    { res.addAll(post.equivalentsUsedIn()); } 
    if (activity != null) 
    { res.addAll(activity.equivalentsUsedIn()); } 
    return res; 
  } 

  public int displayMeasures(PrintWriter out)   
  { String res = ""; 
    String nme = getName(); 
    if (entity != null) 
    { nme = entity.getName() + "::" + nme; } 
    else if (useCase != null) 
    { nme = useCase.getName() + "::" + nme; } 

    int pars = parameters.size(); 

    if (resultType != null)
    { pars++; } 

    out.println("*** Number of parameters of operation " + nme + " = " + pars); 
    if (pars > 10) 
    { System.err.println("*** Bad smell: too many parameters (" + pars + ") for " + nme); }  

    int complexity = 0; 
    int cyc = 0; 

    if (pre != null)
    { complexity = complexity + pre.syntacticComplexity(); } 
    if (post != null) 
    { complexity = complexity + post.syntacticComplexity();
      cyc = post.cyclomaticComplexity(); 
    }

    out.println("*** Postcondition cyclomatic complexity = " + cyc); 
    
    if (activity != null)
    { cyc = activity.cyclomaticComplexity(); 
      complexity = complexity + cyc; 
      out.println("*** Activity cyclomatic complexity = " + cyc); 
    }
    
    out.println("*** Total complexity of operation " + nme + " = " + complexity); 
    out.println(); 
    if (cyc > 10) 
    { System.err.println("*** Bad smell: high cyclomatic complexity (" + cyc + ") for " + nme); }  
    if (complexity > 100) 
    { System.err.println("*** Bad smell: too high complexity (" + complexity + ") for " + nme); }  
    else if (complexity > 50) 
    { System.err.println("*** Warning: high complexity (" + complexity + ") for " + nme); }  

    return complexity; 
  } 

  public int syntacticComplexity()   
  { 
    int complexity = 0; 
    
    if (pre != null)
    { complexity = complexity + pre.syntacticComplexity(); } 
    if (post != null) 
    { complexity = complexity + post.syntacticComplexity(); }
    
    if (activity != null)
    { complexity = complexity + activity.syntacticComplexity(); }
    
    return complexity; 
  } 

  public int cyclomaticComplexity()   
  { String res = ""; 
    String nme = getName(); 
    if (entity != null) 
    { nme = entity.getName() + "::" + nme; } 
    else if (useCase != null) 
    { nme = useCase.getName() + "::" + nme; } 

    int pars = parameters.size(); 

    if (resultType != null)
    { pars++; } 

    System.out.println("*** Number of parameters of operation " + nme + " = " + pars); 
    if (pars > 10) 
    { System.err.println("*** Bad smell: too many parameters (" + pars + ") for " + nme); }  

    int complexity = 0; 
    int cyc = 0; 

    if (pre != null)
    { complexity = complexity + pre.syntacticComplexity(); } 
    if (post != null) 
    { complexity = complexity + post.syntacticComplexity();
      cyc = post.cyclomaticComplexity(); 
    }

    System.out.println("*** Postcondition cyclomatic complexity = " + cyc); 
    
    if (activity != null)
    { cyc = activity.cyclomaticComplexity(); 
      complexity = complexity + cyc; 
      System.out.println("*** Activity cyclomatic complexity = " + cyc); 
    }
    
    System.out.println("*** Total complexity of operation " + nme + " = " + complexity); 
    System.out.println(); 
    if (cyc > 10) 
    { System.err.println("*** Bad smell: high cyclomatic complexity (" + cyc + ") for " + nme); }  
    if (complexity > 100) 
    { System.err.println("*** Bad smell: too high complexity (" + complexity + ") for " + nme); }  

    return cyc; 
  } 

  public int epl() 
  { return parameters.size(); }  // plus variables introduced in post or activity. 

  public int cc()   
  { int res = 0; 

    if (post != null) 
    { res = post.cyclomaticComplexity(); }

    if (activity != null)
    { res = res + activity.cyclomaticComplexity(); }
    
    return res; 
  } 

  public void findClones(java.util.Map clones)
  { String nme = getName(); 
    if (entity != null) 
    { nme = entity.getName() + "::" + nme; } 
    else if (useCase != null) 
    { nme = useCase.getName() + "::" + nme; } 

    if (pre != null) 
    { pre.findClones(clones,null, nme); } 
    if (post != null) 
    { post.findClones(clones,null, nme); } 
  } // and activity

  public void generateJava(PrintWriter out)
  { out.print(toString()); } 

  public void generateCSharp(PrintWriter out)
  { out.print(toString()); } 

  public String genQueryCode(Entity ent, Vector cons)
  { String res = " public "; 
    if (isSequential())
    { res = res + "synchronized "; } 
    if (isAbstract())
    { res = res + "abstract "; } // so comment out code
    if (isClassScope() || isStatic())
    { res = res + "static "; } 

    if (resultType == null) 
    { JOptionPane.showMessageDialog(null, "ERROR: null result type for: " + this,
                                    "Type error", JOptionPane.ERROR_MESSAGE);
      return ""; 
    } 
 
    res = res + resultType.getJava() +
                 " " + getName() + "(";
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getType().getJava() +
                   " " + par.getName();
      if (i < parameters.size() - 1)
      { dec = dec + ", "; }
      res = res + dec;
    }
    if (isAbstract())
    { res = res + ");\n\n"; 
      return res; 
    } 
    res = res + ")\n";
    res = res + "  { " + resultType.getJava() + " " +
          " result = " + resultType.getDefault() +
          ";\n";
    for (int j = 0; j < cons.size(); j++)
    { Constraint con = (Constraint) cons.get(j);
      if (con.getEvent() != null &&
          con.getEvent().equals(this))
      res = res + "    " +
          con.queryOperation(ent) + "\n";
    }
    res = res + "    return result;\n";
    res = res + "  }\n\n";
    return res;
  }

  public String genQueryCodeJava6(Entity ent, Vector cons)
  { String res = " public "; 
    if (isSequential())
    { res = res + "synchronized "; } 
    if (isAbstract())
    { res = res + "abstract "; } // so comment out code
    if (isClassScope() || isStatic())
    { res = res + "static "; } 

    if (resultType == null) 
    { JOptionPane.showMessageDialog(null, "ERROR: null result type for: " + this,
                                    "Type error", JOptionPane.ERROR_MESSAGE);
      return ""; 
    } 

    res = res + resultType.getJava6() +
                 " " + getName() + "(";
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getType().getJava6() +
                   " " + par.getName();
      if (i < parameters.size() - 1)
      { dec = dec + ", "; }
      res = res + dec;
    }
    if (isAbstract())
    { res = res + ");\n\n"; 
      return res; 
    } 
    res = res + ")\n";
    res = res + "  { " + resultType.getJava6() + " " +
          " result = " + resultType.getDefaultJava6() +
          ";\n";

    for (int j = 0; j < cons.size(); j++)
    { Constraint con = (Constraint) cons.get(j);
      if (con.getEvent() != null &&
          con.getEvent().equals(this))
      res = res + "    " +
          con.queryOperationJava6(ent) + "\n";  // Java6 version
    }
    res = res + "    return result;\n";
    res = res + "  }\n\n";
    return res;
  }

  public String genQueryCodeJava7(Entity ent, Vector cons)
  { String res = " public "; 
    if (isSequential())
    { res = res + "synchronized "; } 
    if (isAbstract())
    { res = res + "abstract "; } // so comment out code
    if (isClassScope() || isStatic())
    { res = res + "static "; } 

    if (resultType == null) 
    { JOptionPane.showMessageDialog(null, "ERROR: null result type for: " + this,
                                    "Type error", JOptionPane.ERROR_MESSAGE);
      return ""; 
    } 

    res = res + resultType.getJava7(elementType) +
                 " " + getName() + "(";
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getType().getJava7(par.getElementType()) +
                   " " + par.getName();
      if (i < parameters.size() - 1)
      { dec = dec + ", "; }
      res = res + dec;
    }
    if (isAbstract())
    { res = res + ");\n\n"; 
      return res; 
    } 
    res = res + ")\n";
    res = res + "  { " + resultType.getJava7(elementType) + " " +
          " result = " + resultType.getDefaultJava7() +
          ";\n";

    for (int j = 0; j < cons.size(); j++)
    { Constraint con = (Constraint) cons.get(j);
      if (con.getEvent() != null &&
          con.getEvent().equals(this))
      res = res + "    " +
          con.queryOperationJava7(ent) + "\n";  // Java6 version
    }
    res = res + "    return result;\n";
    res = res + "  }\n\n";
    return res;
  }

  public String genQueryCodeCSharp(Entity ent, Vector cons)
  { String res = " public "; 
    // if (isSequential())
    // { res = res + "synchronized "; } 
    if (isAbstract())
    { res = res + "abstract "; } // so comment out code
    if (isClassScope() || isStatic())
    { res = res + "static "; } 

    if (resultType == null) 
    { JOptionPane.showMessageDialog(null, "ERROR: null result type for: " + this,
                                    "Type error", JOptionPane.ERROR_MESSAGE);
      return ""; 
    } 

    res = res + resultType.getCSharp() +
                 " " + getName() + "(";
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getType().getCSharp() +
                   " " + par.getName();
      if (i < parameters.size() - 1)
      { dec = dec + ", "; }
      res = res + dec;
    }
    if (isAbstract())
    { res = res + ");\n\n"; 
      return res; 
    } 
    res = res + ")\n";
    res = res + "  { " + resultType.getCSharp() + " " +
          " result = " + resultType.getDefaultCSharp() +
          ";\n";
    for (int j = 0; j < cons.size(); j++)
    { Constraint con = (Constraint) cons.get(j);
      if (con.getEvent() != null &&
          con.getEvent().equals(this))
      res = res + "    " +
          con.queryOperationCSharp(ent) + "\n";  // CSharp version
    }
    res = res + "    return result;\n";
    res = res + "  }\n\n";
    return res;
  }

  public String genQueryCodeCPP(Entity ent, Vector cons)
  { String res = ""; 
    // " public "; 
    // if (isSequential())
    // { res = res + "synchronized "; } 
    // if (isAbstract())
    // { res = res + "abstract "; } // so comment out code
    // if (isClassScope() || isStatic())
    // { res = res + "static "; } 

    if (resultType == null) 
    { JOptionPane.showMessageDialog(null, "ERROR: null result type for: " + this,
                                    "Type error", JOptionPane.ERROR_MESSAGE);
      return ""; 
    } 

    String ename = ""; 
    if (entity != null) 
    { ename = entity.getName() + "::"; } 
    else if (ent != null) 
    { ename = ent.getName() + "::"; } 
    
    res = res + resultType.getCPP(getElementType()) +
                 " " + ename + getName() + "(";
    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String dec = par.getType().getCPP(par.getElementType()) +
                   " " + par.getName();
      if (i < parameters.size() - 1)
      { dec = dec + ", "; }
      res = res + dec;
    }
    if (isAbstract())
    { res = res + ");\n\n"; 
      return res; 
    } 
    res = res + ")\n";
    res = res + "  { " + resultType.getCPP(getElementType()) + " " +
          " result = " + resultType.getDefaultCPP(elementType) +
          ";\n";
    for (int j = 0; j < cons.size(); j++)
    { Constraint con = (Constraint) cons.get(j);
      if (con.getEvent() != null &&
          con.getEvent().equals(this))
      res = res + "    " +
          con.queryOperationCPP(ent) + "\n";  // CPP version
    }
    res = res + "    return result;\n";
    res = res + "  }\n\n";
    return res;
  }

  public void saveData(PrintWriter out)
  { if (derived) { return; } 
    out.println("Operation:");
    out.println(getName());
    if (useCase != null) 
    { out.println("null " + useCase.getName()); } 
    else 
    { out.println(entity); }  

    if (resultType == null)
    { out.println("void"); }
    else
    { out.println(resultType); } 
    /* String rtname = resultType.getName(); 
      if (rtname.equals("Set") || rtname.equals("Sequence"))
      { out.println(rtname + "(" + elementType + ")"); } 
      else 
      { out.println(rtname); }
    } */ 

    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String pnme = par.getName();
      // String tnme = par.getType().getName();
      // String elemt = ""; 
      // if ("Set".equals(tnme) || "Sequence".equals(tnme))
      // { if (par.getElementType() != null) 
      //   { elemt = "(" + par.getElementType() + ")"; } 
      // } 
      out.print(pnme + " " + par.getType() + " ");
    }
    out.println(); 
    
    String ss = "";  
    for (int i = 0; i < stereotypes.size(); i++)
    { ss = ss + " " + stereotypes.get(i); } 
    out.println(ss);
    out.println(pre);
    out.println(post); 
    out.println(""); 
    out.println(""); 
  }  // multiple pre, post, and stereotypes, and the operation text

  public void saveKM3(PrintWriter out)
  { if (derived) { return; } 

    if (isStatic())
    { out.print("    static operation "); } 
    else 
    { out.print("    operation "); } 

    out.print(getName() + "(");

    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String pnme = par.getName();
      out.print(pnme + " : " + par.getType() + " ");
      if (i < parameters.size() - 1)
      { out.print(", "); } 
    }
    out.print(") : "); 

    if (resultType == null)
    { out.println("void"); }
    else
    { out.println(resultType); } 

    if (pre != null) 
    { out.println("    pre: " + pre); } 
    else 
    { out.println("    pre: true"); } 
     
    if (activity != null) 
    { out.println("    post: " + post);
      out.println("    activity: " + activity + ";"); 
    } 
    else 
    { out.println("    post: " + post + ";"); } 
      
    out.println(""); 
  } 

  public String getKM3()
  { if (derived) { return ""; } 
    String res = ""; 

    if (isStatic())
    { res = "    static operation "; } 
    else 
    { res = "    operation "; }
 
    res = res + getName() + "(";

    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String pnme = par.getName();
      res = res + pnme + " : " + par.getType() + " ";
      if (i < parameters.size() - 1)
      { res = res + ", "; } 
    }
    res = res + ") : "; 

    if (resultType == null)
    { res = res + "void\n"; }
    else
    { res = res + resultType + "\n"; } 

    if (pre != null) 
    { res = res + "    pre: " + pre + "\n"; } 
    else 
    { res = res + "    pre: true\n"; } 
     
    if (activity != null) 
    { res = res + "    post: " + post + "\n";
      res = res + "    activity: " + activity + ";\n"; 
    } 
    else 
    { res = res + "    post: " + post + ";\n"; } 
    res = res + "\n";
    return res;  
  } 

  public String saveModelData(PrintWriter out, Entity ent, Vector entities, Vector types)
  { if (stereotypes.contains("auxiliary"))
    { return ""; } // but derived ops are usually recorded. 

    String nme = getName();
    String opid = Identifier.nextIdentifier("operation_"); 
    out.println(opid + " : Operation"); 
    out.println(opid + ".name = \"" + nme + "\""); 
    Vector cntxt = new Vector(); 
    Vector env = new Vector(); 
    env.addAll(parameters); 

    if (useCase != null) 
    { out.println(opid + ".useCase = " + useCase.getName()); } 
    else if (entity != null) 
    { out.println(opid + ".owner = " + entity); 
      // out.println(opid + " : " + entity + ".ownedOperation"); 
      cntxt.add(entity); 
    }  

    if (isQuery())
    { out.println(opid + ".isQuery = true"); } 
    else 
    { out.println(opid + ".isQuery = false"); } 

    if (isAbstract())
    { out.println(opid + ".isAbstract = true"); } 
    else 
    { out.println(opid + ".isAbstract = false"); } 

    if (isStatic())
    { out.println(opid + ".isStatic = true"); } 
    else 
    { out.println(opid + ".isStatic = false"); } 

    if (isCached())
    { out.println(opid + ".isCached = true"); } 
    else 
    { out.println(opid + ".isCached = false"); } 

    if (resultType == null)
    { out.println(opid + ".type = void");
      out.println(opid + ".elementType = void"); 
    }
    else
    { String rtname = resultType.getName();
      String typeid = resultType.getUMLModelName(out); 
      out.println(opid + ".type = " + typeid);  
      if (rtname.equals("Set") || rtname.equals("Sequence"))
      { if (elementType == null) 
        { System.err.println("Error: No element type for " + this); 
          out.println(opid + ".elementType = void"); 
        } 
        else 
        { String elemtid = elementType.getUMLModelName(out); 
          out.println(opid + ".elementType = " + elemtid);
        }  
      } 
      else  
      { out.println(opid + ".elementType = " + typeid); } 
    } 

    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String pid = par.saveModelData(out); 
      // String pnme = par.getName();
      out.println(pid + " : " + opid + ".parameters");
    }
    
    for (int i = 0; i < stereotypes.size(); i++)
    { out.println("\"" + stereotypes.get(i) + "\" : " + opid + ".stereotypes"); }
 
    String preid; 
    if (pre != null) 
    { preid = pre.saveModelData(out); } 
    else 
    { preid = "true"; } 
    String postid; 
    if (post != null) 
    { postid = post.saveModelData(out); } 
    else 
    { postid = "true"; }  
    out.println(opid + ".precondition = " + preid);
    out.println(opid + ".postcondition = " + postid);

    if (activity == null) 
    { Statement des = generateDesign(ent, entities, types); 
      des.typeCheck(types,entities,cntxt,env); 
      String desid = des.saveModelData(out); 
      out.println(opid + ".activity = " + desid); 
      System.out.println("Operation activity is: " + des); 
    } 
    else // if (activity != null) 
    { String actid = activity.saveModelData(out); 
      out.println(opid + ".activity = " + actid); 
    }  
    // out.println(opid + ".text = \"" + text + "\""); 

   
    return opid;  
  }  // multiple pre, post, and stereotypes, and the operation text

  public String saveAsUSEData()
  { if (derived) { return ""; } 

    String res = getName() + "(";

    for (int i = 0; i < parameters.size(); i++)
    { Attribute par = (Attribute) parameters.get(i);
      String pnme = par.getName();
      String tnme = par.getType().getUMLName();
      String elemt = ""; 
      if ("Set".equals(tnme) || "Sequence".equals(tnme))
      { if (par.getElementType() != null) 
        { elemt = "(" + par.getElementType().getUMLName() + ")"; } 
      } 
      res = res + pnme + " : " + tnme + elemt;
      if (i < parameters.size() - 1) { res = res + ", "; } 
    }
    res = res + ") "; 
    if (resultType != null)
    { String rtname = resultType.getName(); 
      if (rtname.equals("Set") || rtname.equals("Sequence"))
      { if (elementType != null) 
        { res = res + " : " + resultType.getUMLName() + "(" + elementType.getUMLName() + ")"; } 
        else 
        { res = res + " : " + resultType.getUMLName() + "()"; } 
      } 
      else 
      { res = res + " : " + resultType.getUMLName(); }
    } 
    res = res + "\n"; 
    
    java.util.Map env = new java.util.HashMap(); 

    if (pre == null) 
    { pre = new BasicExpression("true"); }  
    
    if (post == null) 
    { post = new BasicExpression("true"); } 

    res = res + "pre pre1:\n  " + pre.toOcl(env,true) + "\n";
    res = res + "post post1:\n   " + post.toOcl(env,true); 
    res = res + "\n\n"; 
    return res; 
  }  // multiple pre, post, and stereotypes


  public static Expression wpc(Expression pst, String op, String feat, 
                               String val)
  { // calculates [opfeat(val)]pst 
    Expression res = pst; 
    BasicExpression valbe = new BasicExpression(val);  
    if (op.equals("set"))
    { // System.out.println(valbe + " multiplicity is: " + valbe.isMultiple()); 
      res = pst.substituteEq(feat,valbe); 
    } 
    else if (op.equals("add"))
    { SetExpression se = new SetExpression(); 
      se.addElement(valbe); 
      Expression featbe = new BasicExpression(feat); 
      res = pst.substituteEq(feat,new BinaryExpression("\\/",featbe,se)); 
    }
    else if (op.equals("remove"))
    { SetExpression se = new SetExpression(); 
      se.addElement(valbe); 
      Expression featbe = new BasicExpression(feat); 
      res = pst.substituteEq(feat,new BinaryExpression("-",featbe,se)); 
    }
    else if (op.equals("union"))
    { Expression featbe = new BasicExpression(feat); 
      res = pst.substituteEq(feat,new BinaryExpression("\\/",featbe,valbe)); 
    }
    else if (op.equals("subtract"))
    { Expression featbe = new BasicExpression(feat); 
      res = pst.substituteEq(feat,new BinaryExpression("-",featbe,valbe)); 
    }

    return res; 
  }

  /*
  public static Expression wpc(Expression post, Vector uses, Expression exbe,
                               String op, String feat,
                               BasicExpression valbe)
  { if (uses.size() == 0) { return post; }
    Expression nextpost = post; 
  
    if (op.equals("set"))
    { nextpost = post.substituteEq(feat,valbe); } 
    else if (op.equals("add")) // replace feat by feat \/ { valbe } for add, etc
    { Expression featbe = new BasicExpression(feat); 
      SetExpression setexp = new SetExpression(); 
      setexp.addElement(valbe); 
      nextpost = post.substituteEq(feat,new BinaryExpression("\\/",featbe,setexp)); 
    }
    else if (op.equals("remove"))  // replace feat by feat - { valbe } 
    { Expression featbe = new BasicExpression(feat); 
      SetExpression setexp = new SetExpression(); 
      setexp.addElement(valbe); 
      nextpost = post.substituteEq(feat,new BinaryExpression("-",featbe,setexp)); 
    } // do these ONCE only
    return wpc0(nextpost,uses,exbe,op,feat,valbe); 
  } */   

  // Not checked, may need to be updated
  public static Expression wpc(Expression post, Vector uses, Expression exbe,
                               String op, String feat, boolean ord, 
                               BasicExpression valbe)
  { if (uses.size() == 0) { return post; }
    Expression pre = post; 
    BasicExpression use = (BasicExpression) uses.get(0); 

    if (use.objectRef == null || use.objectRef.umlkind == Expression.CLASSID)
    { if (op.equals("set"))
      { pre = post.substitute(use,valbe); }  // use is just feat 
      else if (op.equals("add")) // replace feat by feat \/ { valbe } for add, etc
      { Expression featbe = (BasicExpression) use.clone();
                          // new BasicExpression(feat);
        SetExpression setexp = new SetExpression(); 
        setexp.addElement(valbe); 
        Expression repl; 
        if (ord)
        { setexp.setOrdered(true); 
          repl = new BinaryExpression("^",featbe,setexp);
        } 
        else 
        { repl = new BinaryExpression("\\/",featbe,setexp); }
          
        repl.setBrackets(true); 
        pre = post.substitute(use,repl); 
      }
      else if (op.equals("remove"))  // replace feat by feat - { valbe } 
      { Expression featbe = new BasicExpression(feat); 
        SetExpression setexp = new SetExpression(); 
        setexp.addElement(valbe); 
        Expression repl = new BinaryExpression("-",featbe,setexp); 
        repl.setBrackets(true); 
        pre = post.substitute(use,repl); 
      } // do these ONCE only
      else if (op.equals("union"))
      { Expression featbe = (BasicExpression) use.clone();
                          // new BasicExpression(feat);
        Expression repl;
        if (ord)
        { repl = new BinaryExpression("^",featbe,valbe); }
        else 
        { repl = new BinaryExpression("\\/",featbe,valbe); }
        repl.setBrackets(true); 
        pre = post.substitute(use,repl); 
      }
      else if (op.equals("subtract"))  // replace feat by feat - valbe 
      { Expression featbe = new BasicExpression(feat); 
        Expression repl = new BinaryExpression("-",featbe,valbe); 
        repl.setBrackets(true); 
        pre = post.substitute(use,repl); 
      } // do these ONCE only

      // Expression selfeq = 
      //   new BinaryExpression("=",new BasicExpression("self"),
      //                        exbe); 
      // pre = Expression.simplifyImp(selfeq,pre);       
      uses.remove(0);
      return wpc(pre,uses,exbe,op,feat,ord,valbe);
    }

    if (use.objectRef.umlkind == Expression.VARIABLE)
    { pre = pre.substitute(use,valbe);
      pre = pre.substituteEq(use.objectRef.toString(),exbe);  
      // Expression neq = new BinaryExpression("/=",exbe,use.objectRef); 
      // Expression pre2 = new BinaryExpression("&",neq,pre); 
      // pre = new BinaryExpression("or",pre1,pre2);
      // pre.setBrackets(true);  
      // Only if set? 
    }
    else if (!use.objectRef.isMultiple())
    { Expression pre0 = pre.substitute(use,valbe); // also in above case?
      BinaryExpression eq =
        new BinaryExpression("=",exbe,use.objectRef);
      pre = Expression.simplify("=>",eq,pre0,new Vector());
     // Expression neq = new BinaryExpression("/=",exbe,use.objectRef); 
     // Expression pre2 = new BinaryExpression("&",neq,pre); 
     // pre = new BinaryExpression("or",pre1,pre2);  
     // pre.setBrackets(true); 
    }      
    else if (use.objectRef.isMultiple())  // objs.f
    { if (op.equals("set"))
      { if (ord && use.objectRef.isOrdered())
        { // just objs.(f <+ { ex |-> value })
          BasicExpression fbe = 
            new BasicExpression(feat);
          Expression maplet = new BinaryExpression("|->",exbe,valbe); 
        
          SetExpression se = new SetExpression();
          se.addElement(maplet);
          BinaryExpression oplus = new BinaryExpression("<+",fbe,se);
          oplus.setBrackets(true);  
          oplus.objectRef = use.objectRef;
          pre = pre.substitute(use,oplus);   
        }  
        else
        { SetExpression se = new SetExpression();
          se.addElement(exbe);
          Expression newobjs =
              new BinaryExpression("-",use.objectRef,se);
          newobjs.setBrackets(true); 
          BasicExpression fbe = 
              new BasicExpression(feat);
          fbe.setObjectRef(newobjs);
          if (use.umlkind == Expression.ATTRIBUTE ||
              use.multiplicity == ModelElement.ONE)
          { // replace objs.f by (objs - {ex}).f \/ {value}
            SetExpression vse = new SetExpression();
            vse.addElement(valbe);
            Expression repl = 
              new BinaryExpression("\\/",fbe,vse);
            repl.setBrackets(true); 
            pre = pre.substitute(use,repl);   
            // and implied by exbe: objs
          }
          else if (use.umlkind == Expression.ROLE &&
                   use.multiplicity != ModelElement.ONE) 
          { Expression repl = 
              new BinaryExpression("\\/",fbe,valbe);
            repl.setBrackets(true); 
            pre = pre.substitute(use,repl); 
          }
        }
        Expression exin = new BinaryExpression(":",exbe,use.objectRef); // : ran ?
        pre = Expression.simplifyImp(exin,pre);   
        uses.remove(0);
        return wpc(pre,uses,exbe,op,feat,ord,valbe);
      }
      else if (op.equals("add"))
      { if (ord && use.objectRef.isOrdered())
        { // just objs.(f <+ { ex |-> f(ex) ^ [value] })
          BasicExpression fbe1 = 
            new BasicExpression(feat);
          BasicExpression fbe = 
            new BasicExpression(feat);
  
          BasicExpression exbe1 = (BasicExpression) exbe.clone(); 
          exbe1.objectRef = fbe1;  // f(ex)
          SetExpression sq = new SetExpression(); 
          sq.setOrdered(true); 
          sq.addElement(valbe);  // [val]
          Expression cat = new BinaryExpression("^",exbe1,sq); 
  
          Expression maplet = new BinaryExpression("|->",exbe,cat); 
        
          SetExpression se = new SetExpression();
          se.addElement(maplet);
          BinaryExpression oplus = new BinaryExpression("<+",fbe,se);
          oplus.setBrackets(true);  
          oplus.objectRef = use.objectRef;
          pre = pre.substitute(use,oplus);   
        }  
        else 
        { SetExpression vse = new SetExpression();
          vse.addElement(valbe);
          Expression repl = 
            new BinaryExpression("\\/",use,vse);
          repl.setBrackets(true); 
          pre = pre.substitute(use,repl);   
        }
      } // exbe: objs for pre
      else if (op.equals("union"))
      { Expression repl = 
          new BinaryExpression("\\/",use,valbe);
        repl.setBrackets(true); 
        pre = pre.substitute(use,repl);   
      } // exbe: objs for pre
      else if (op.equals("remove"))
      { SetExpression se = new SetExpression();
        se.addElement(exbe);
        Expression newobjs = // (objs - {ex}).f
          new BinaryExpression("-",use.objectRef,se);
        newobjs.setBrackets(true); 
        BasicExpression fbe = 
          new BasicExpression(feat);
        fbe.setObjectRef(newobjs);
        SetExpression vse = new SetExpression();
        vse.addElement(valbe);    // { value }
        BasicExpression fbe2 = new BasicExpression(feat);
        fbe2.setObjectRef(exbe);  // ex.f
        Expression minus = 
          new BinaryExpression("-",fbe2,vse);
        minus.setBrackets(true); 
        Expression repl = 
          new BinaryExpression("\\/",fbe,minus);
        repl.setBrackets(true); 
        pre = pre.substitute(use,repl);   
      } 
      else if (op.equals("subtract"))
      { SetExpression se = new SetExpression();
        se.addElement(exbe);
        Expression newobjs = // (objs - {ex}).f
          new BinaryExpression("-",use.objectRef,se);
        newobjs.setBrackets(true); 
        BasicExpression fbe = 
          new BasicExpression(feat);
        fbe.setObjectRef(newobjs);
        BasicExpression fbe2 = new BasicExpression(feat);
        fbe2.setObjectRef(exbe);  // ex.f
        Expression minus = 
          new BinaryExpression("-",fbe2,valbe);
        minus.setBrackets(true); 
        Expression repl = 
          new BinaryExpression("\\/",fbe,minus);
        repl.setBrackets(true); 
        pre = pre.substitute(use,repl);   
      }  // union, subtract?
      
      Expression inexp = new BinaryExpression(":",exbe,use.objectRef); 
      pre = new BinaryExpression("=>",inexp,pre);
    } // or exbe /: objs & this
    uses.remove(0); 
    return wpc(pre,uses,exbe,op,feat,ord,valbe); 
  }

  public Statement generateDesign(Entity ent, Vector entities, Vector types)
  { Statement res = new SequenceStatement(); 

    String name = getName();
    String ename = ""; 
    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent);  
      ename = ent.getName(); 
    } 

    String ex = ename.toLowerCase() + "x";    // very messy to do this 
    java.util.Map env0 = new java.util.HashMap(); 
    String pars = getJavaParameterDec(); 

    // String header = 

    if ("run".equals(name) && ent != null && ent.isActive())
    { if (ent.isInterface())
      { return res; } 

      if (sm != null) 
      { Statement rc = sm.runCode(); 
        // add run_state : T to ent, and the type. 
        rc.typeCheck(types,entities,context,new Vector()); 
        return rc; 
      } // should be rc.updateForm(env0,true)
      return res; 
    }  


    if (sm != null) 
    { Statement cde = sm.getMethodJava(); // sequential code
      return cde; 
    } 
    else if (activity != null) 
    { return activity; } 

    Vector atts = new Vector(); 
    ReturnStatement rets = null; 
  
    String resT = "void";
    if (post == null) 
    { return res; }   // or use the SM if one exists

    if (resultType != null && !("void".equals(resultType.getName())))
    { resT = resultType + ""; 
      Attribute r = new Attribute("result", resultType, ModelElement.INTERNAL); 
      r.setElementType(elementType); 
      atts.add(r); 
      BasicExpression rbe = new BasicExpression(r); 
      CreationStatement cs = new CreationStatement(resT, "result"); 
      cs.setType(resultType); 
      cs.setElementType(elementType); 
      Expression init = resultType.getDefaultValueExpression(elementType); 
      AssignStatement initialise = new AssignStatement(rbe, init); 
      ((SequenceStatement) res).addStatement(cs); 
      ((SequenceStatement) res).addStatement(initialise); 
      rets = new ReturnStatement(rbe); 
    }

    if (ent != null && ent.isInterface())
    { return res; } 

    atts.addAll(parameters); 

    if (query)
    { Vector cases = Expression.caselist(post); 
      Statement qstat = designQueryList(cases, resT, env0, types, entities, atts);
      ((SequenceStatement) res).addStatement(qstat); 
      ((SequenceStatement) res).addStatement(rets); 
      return res; 
    } 
    else 
    { Statement stat; 
      if (bx) 
      { stat = post.statLC(env0,true); } 
      else 
      { stat = post.generateDesign(env0, true); } 
 
      ((SequenceStatement) res).addStatement(stat); 
      if (resultType != null && !("void".equals(resultType.getName()))) 
      { ((SequenceStatement) res).addStatement(rets); }  

      return res; 
    } // if no return 

    // return res; 

    // { return buildQueryOp(ent,name,resultType,resT,entities,types); }
    // else 
    // { return buildUpdateOp(ent,name,resultType,resT,entities,types); }
  } // ignore return type for update ops for now. 


  // For the entity operation: ent == entity normally 
  public String getOperationCode(Entity ent, Vector entities, Vector types)
  { String name = getName();
    String ename = ""; 
    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent);  
      ename = ent.getName(); 
    } 

    String ex = ename.toLowerCase() + "x";    // very messy to do this 
    java.util.Map env0 = new java.util.HashMap(); 
    String pars = getJavaParameterDec(); 

    // String header = 

    String textcode = ""; 
    if (hasText())
    { textcode = text + "\n"; } 
  

    if ("run".equals(name) && ent != null && ent.isActive())
    { if (ent.isInterface())
      { return "  public void run();\n"; } 

      String res = "  public void run()\n  { "; 
      if (sm != null) 
      { Statement rc = sm.runCode(); 
        // add run_state : T to ent, and the type. 
        rc.typeCheck(types,entities,context,new Vector()); 
        return res + rc.toStringJava() + "  }\n\n"; 
      } // should be rc.updateForm(env0,true)
      res = res + textcode; 
      return res + " }\n\n"; 
    }  


    if (sm != null) 
    { Statement cde = sm.getMethodJava(); // sequential code
      if (cde != null) 
      { Vector localatts = new Vector(); 
        localatts.addAll(sm.getAttributes()); 
        String res = ""; 
        Type tp = getResultType(); 
        String ts = "void";
        String statc = ""; 

        if (tp != null) 
        { ts = tp.getJava(); } 

        if (isClassScope() || isStatic()) 
        { statc = "static "; } 

        res = "  public " + statc + ts + " " + name + "(" + pars + ")\n  { "; 

        if (ent.isInterface())
        { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 
 
        if (tp != null)
        { res = res + ts + " result;\n"; }
        localatts.addAll(parameters);   
        cde.typeCheck(types,entities,context,localatts); 
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + cde.updateForm(env0,true,types,entities,localatts) + "\n  }";
      }   // cde.updateForm(env0,true)
    } 
    else if (activity != null) 
    { Vector localatts = new Vector(); 
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { env0.put(ename,ex); } //  or "this" if not static ??
      String res = ""; 

      Type tp = getResultType(); 
      String ts = "void";

      String header = ""; 

      if (tp != null) 
      { ts = tp.getJava(); } 

      if (ent != null && ent.isInterface())
      { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 

      if (isSequential())
      { header = header + "synchronized "; } 
      if (isAbstract())
      { header = header + "abstract "; } 
      if (isFinal())
      { header = header + "final "; } 
      if (isClassScope() || isStatic())
      { header = header + "static "; } 

      res = "  public " + header + ts + " " + name + "(" + pars + ")\n  {  "; 

      if (tp != null)
      { res = res + ts + " result;\n"; }
      localatts.addAll(parameters);   

      Vector preterms = activity.allPreTerms(); // But some should be within the activity
      if (preterms.size() > 0) 
      { Statement newpost = (Statement) activity.clone(); 
        // System.out.println("PRE terms: " + preterms); 
        Vector processed = new Vector(); 
        String newdecs = ""; 
        for (int i = 0; i < preterms.size(); i++)
        { BasicExpression preterm = (BasicExpression) preterms.get(i);
          if (processed.contains(preterm)) { continue; }  
          // check if the preterm is valid and skip if not. 
          Type typ = preterm.getType();  // but actual type may be list if multiple
          Type actualtyp; 
          String newdec = ""; 
          String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
          String pretermqf = preterm.queryForm(env0,true); 
                             // classqueryForm; you want to remove the @pre

          BasicExpression prebe = new BasicExpression(pre_var); 

          if (preterm.isMultiple())
          { if (preterm.isOrdered())
            { actualtyp = new Type("Sequence",null); } 
            else 
            { actualtyp = new Type("Set",null); } 
            actualtyp.setElementType(preterm.getElementType()); 

            if (preterm.umlkind == Expression.CLASSID && preterm.arrayIndex == null) 
            { pretermqf = "Controller.inst()." + pretermqf.toLowerCase() + "s"; } 
            newdec = actualtyp.getJava() + " " + pre_var + " = new Vector();\n" + 
                 "    " + pre_var + ".addAll(" + pretermqf + ");\n"; 
          } // Use this strategy also below and in Statement.java 
          else 
          { actualtyp = typ;
            newdec = actualtyp.getJava() + " " + pre_var + " = " + pretermqf + ";\n";
          } 
          newdecs = newdecs + "    " + newdec; 
          prebe.type = actualtyp; 
          prebe.elementType = preterm.elementType; 
          // System.out.println("$$$ PRE variable " + prebe + " type= " + actualtyp + 
          //                     " elemtype= " + prebe.elementType + " FOR " + preterm); 
          
          Attribute preatt = 
            new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
          preatt.setElementType(preterm.elementType); 
          localatts.add(preatt); 
          newpost = newpost.substituteEq("" + preterm,prebe); 
          processed.add(preterm); 
        }  // substitute(preterm,prebe) more appropriate 

        newpost.typeCheck(types,entities,context,localatts);
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + newdecs + newpost.updateForm(env0,false,types,entities,localatts) + "\n  }\n";   
      }     // updateForm(, true)? 
      activity.typeCheck(types,entities,context,localatts);
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { res = res + ename + " " + ex + " = this;\n  "; }   
      return res + activity.updateForm(env0,false,types,entities,localatts) + "\n  }\n";   
    } 
      
    String resT = "void";
    if (post == null) 
    { return ""; }   // or use the SM if one exists
    if (resultType != null)
    { resT = resultType.getJava(); }

    if (ent != null && ent.isInterface())
    { return "  public " + resT + " " + name + "(" + pars + ");\n"; } 

    if (query)
    { return buildQueryOp(ent,name,resultType,resT,entities,types); }
    else 
    { return buildUpdateOp(ent,name,resultType,resT,entities,types); }
  } // ignore return type for update ops for now. 

  public String getOperationCodeJava6(Entity ent, Vector entities, Vector types)
  { String name = getName();
    Vector context = new Vector(); 
    String ename = ""; 
    if (ent != null) 
    { context.add(ent); 
      ename = ent.getName(); 
    } 
    String ex = ename.toLowerCase() + "x"; 
    java.util.Map env0 = new java.util.HashMap(); 
    String pars = getJava6ParameterDec(); 

    String textcode = ""; 
    if (hasText())
    { textcode = text + "\n"; } 
    

    if ("run".equals(name) && ent != null && ent.isActive())
    { if (ent.isInterface())
      { return "  public void run();\n"; } 

      String res = "  public void run()\n  { "; 
      if (sm != null) 
      { Statement rc = sm.runCode(); 
        // add run_state : T to ent, and the type. 
        rc.typeCheck(types,entities,context,new Vector()); 
        return res + rc.updateFormJava6(env0,true) + "  }\n\n"; 
      } 
      res = res + textcode; 
      return res + " }\n\n"; 
    }  


    if (sm != null) 
    { Statement cde = sm.getSequentialCode(); // sequential code
      if (cde != null) 
      { Vector localatts = new Vector(); 
        localatts.addAll(sm.getAttributes()); 
        String res = ""; 
        Type tp = getResultType(); 

        String ts = "void";
        if (tp != null) 
        { ts = tp.getJava6(); } 

        String statc = ""; 
        if (isClassScope() || isStatic()) 
        { statc = "static "; } 


        res = "  public " + statc + ts + " " + name + "(" + pars + ")\n{  "; 

        if (ent != null && ent.isInterface())
        { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 
 
        if (tp != null)
        { res = res + ts + " result;\n"; }
        localatts.addAll(parameters);   
        cde.typeCheck(types,entities,context,localatts); 
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + cde.updateFormJava6(env0,true) + "\n  }";
      }   
    } 
    else if (activity != null) 
    { Vector localatts = new Vector(); 
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { env0.put(ename,ex); } //  or "this" if not static ??
      String res = ""; 

      Type tp = getResultType(); 
      String ts = "void";

      String header = ""; 

      if (tp != null) 
      { ts = tp.getJava6(); } 

      if (ent != null && ent.isInterface())
      { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 

      if (isSequential())
      { header = header + "synchronized "; } 
      if (isAbstract())
      { header = header + "abstract "; } 
      if (isFinal())
      { header = header + "final "; } 
      if (isClassScope() || isStatic())
      { header = header + "static "; } 

      res = "  public " + header + ts + " " + name + "(" + pars + ")\n  {  "; 

      if (tp != null)
      { res = res + ts + " result;\n"; }
      localatts.addAll(parameters);   
      Vector preterms = activity.allPreTerms(); 
      if (preterms.size() > 0) 
      { Statement newpost = (Statement) activity.clone(); 
        // System.out.println("Pre terms: " + preterms); 
        Vector processed = new Vector(); 
        String newdecs = ""; 
        for (int i = 0; i < preterms.size(); i++)
        { BasicExpression preterm = (BasicExpression) preterms.get(i);
          if (processed.contains(preterm)) { continue; }  
          Type typ = preterm.getType();  // but actual type may be list if multiple
          Type actualtyp; 
          String newdec = ""; 
          String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
          String pretermqf = preterm.classqueryFormJava6(env0,true); 
          BasicExpression prebe = new BasicExpression(pre_var); 

          if (preterm.isMultiple())
          { if (preterm.isOrdered())
            { actualtyp = new Type("Sequence",null); 
              newdec =  "ArrayList " + pre_var + " = new ArrayList();\n" + 
                     "    " + pre_var + ".addAll(" + pretermqf + ");\n";
            } 
            else 
            { actualtyp = new Type("Set",null); 
              newdec =  "HashSet " + pre_var + " = new HashSet();\n" + 
                     "    " + pre_var + ".addAll(" + pretermqf + ");\n";
            }  
            actualtyp.setElementType(preterm.getElementType()); 
          } 
          else 
          { actualtyp = typ;
            newdec = actualtyp.getJava6() + " " + pre_var + " = " + pretermqf + ";\n";
          } 
          newdecs = newdecs + "    " + newdec; 
          prebe.type = actualtyp; 
          prebe.elementType = preterm.elementType; 
          // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
          //                    " elemtype= " + prebe.elementType); 
          
          Attribute preatt = 
            new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
          preatt.setElementType(preterm.elementType); 
          localatts.add(preatt); 
          newpost = newpost.substituteEq("" + preterm,prebe); 
          processed.add(preterm); 
        }  // substitute(preterm,prebe) more appropriate 

        newpost.typeCheck(types,entities,context,localatts);
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + newdecs + newpost.updateFormJava6(env0,false) + "\n  }\n";   
      }     // updateForm(, true)? 
      activity.typeCheck(types,entities,context,localatts);
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { res = res + ename + " " + ex + " = this;\n  "; }   
      return res + activity.updateFormJava6(env0,false) + "\n  }\n";   
    } 
      
    String resT = "void";
    if (post == null) 
    { return ""; }   // or use the SM if one exists
    if (resultType != null)
    { resT = resultType.getJava6(); }

    if (ent != null && ent.isInterface())
    { return "  public " + resT + " " + name + "(" + pars + ");\n"; } 

    if (query)
    { return buildQueryOpJava6(ent,name,resultType,resT,entities,types); }
    else 
    { return buildUpdateOpJava6(ent,name,resultType,resT,entities,types); }
  } // ignore return type for update ops for now. 

  public String getOperationCodeJava7(Entity ent, Vector entities, Vector types)
  { String name = getName();
    Vector context = new Vector(); 
    String ename = ""; 
    if (ent != null) 
    { context.add(ent); 
      ename = ent.getName(); 
    } 
    String ex = ename.toLowerCase() + "x"; 
    java.util.Map env0 = new java.util.HashMap(); 
    String pars = getJava7ParameterDec(); 

    String textcode = ""; 
    if (hasText())
    { textcode = text + "\n"; } 
 

    if ("run".equals(name) && ent != null && ent.isActive())
    { if (ent.isInterface())
      { return "  public void run();\n"; } 

      String res = "  public void run()\n  { "; 
      if (sm != null) 
      { Statement rc = sm.runCode(); 
        // add run_state : T to ent, and the type. 
        rc.typeCheck(types,entities,context,new Vector()); 
        return res + rc.updateFormJava7(env0,true) + "  }\n\n"; 
      } 
      res = res + textcode; 
      return res + " }\n\n"; 
    }  


    if (sm != null) 
    { Statement cde = sm.getSequentialCode(); // sequential code
      if (cde != null) 
      { Vector localatts = new Vector(); 
        localatts.addAll(sm.getAttributes()); 
        String res = ""; 
        Type tp = getResultType(); 

        String ts = "void";
        if (tp != null) 
        { ts = tp.getJava7(elementType); } 

        String statc = ""; 
        if (isClassScope() || isStatic()) 
        { statc = "static "; } 


        res = "  public " + statc + ts + " " + name + "(" + pars + ")\n{  "; 

        if (ent != null && ent.isInterface())
        { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 
 
        if (tp != null)
        { res = res + ts + " result;\n"; }
        localatts.addAll(parameters);   
        cde.typeCheck(types,entities,context,localatts); 
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + cde.updateFormJava7(env0,true) + "\n  }";
      }   
    } 
    else if (activity != null) 
    { Vector localatts = new Vector(); 
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { env0.put(ename,ex); } //  or "this" if not static ??
      String res = ""; 

      Type tp = getResultType(); 
      String ts = "void";

      String header = ""; 

      if (tp != null) 
      { ts = tp.getJava7(elementType); } 

      if (ent != null && ent.isInterface())
      { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 

      if (isSequential())
      { header = header + "synchronized "; } 
      if (isAbstract())
      { header = header + "abstract "; } 
      if (isFinal())
      { header = header + "final "; } 
      if (isClassScope() || isStatic())
      { header = header + "static "; } 

      res = "  public " + header + ts + " " + name + "(" + pars + ")\n  {  "; 

      if (tp != null)
      { res = res + ts + " result;\n"; }
      localatts.addAll(parameters);   
      Vector preterms = activity.allPreTerms(); 
      if (preterms.size() > 0) 
      { Statement newpost = (Statement) activity.clone(); 
        // System.out.println("Pre terms: " + preterms); 
        Vector processed = new Vector(); 
        String newdecs = ""; 
        for (int i = 0; i < preterms.size(); i++)
        { BasicExpression preterm = (BasicExpression) preterms.get(i);
          if (processed.contains(preterm)) { continue; }  
          Type typ = preterm.getType();  // but actual type may be list if multiple
          Type actualtyp; 
          String newdec = ""; 
          String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
          String pretermqf = preterm.classqueryFormJava7(env0,true); 
          BasicExpression prebe = new BasicExpression(pre_var); 

          if (preterm.isMultiple())
          { if (preterm.isOrdered())
            { actualtyp = new Type("Sequence",null); 
              actualtyp.setElementType(preterm.getElementType());
              String jType = actualtyp.getJava7(preterm.getElementType()); 
              newdec =  "    " + jType + " " + pre_var + " = new " + jType + "();\n" + 
                        "    " + pre_var + ".addAll(" + pretermqf + ");\n";
            } 
            else 
            { actualtyp = new Type("Set",null); 
              actualtyp.setElementType(preterm.getElementType());
              String jType = actualtyp.getJava7(preterm.getElementType()); 
              newdec = "    " + jType + " " + pre_var + " = new " + jType + "();\n" + 
                       "    " + pre_var + ".addAll(" + pretermqf + ");\n";
            }  
             
          } 
          else 
          { actualtyp = typ;
            newdec = actualtyp.getJava7(preterm.elementType) + " " + pre_var + " = " + pretermqf + ";\n";
          } 
          newdecs = newdecs + "    " + newdec; 
          prebe.type = actualtyp; 
          prebe.elementType = preterm.elementType; 
          // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
          //                    " elemtype= " + prebe.elementType); 
          
          Attribute preatt = 
            new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
          preatt.setElementType(preterm.elementType); 
          localatts.add(preatt); 
          newpost = newpost.substituteEq("" + preterm,prebe); 
          processed.add(preterm); 
        }  // substitute(preterm,prebe) more appropriate 

        newpost.typeCheck(types,entities,context,localatts);
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + newdecs + newpost.updateFormJava7(env0,false) + "\n  }\n";   
      }     // updateForm(, true)? 
      activity.typeCheck(types,entities,context,localatts);
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { res = res + ename + " " + ex + " = this;\n  "; }   
      return res + activity.updateFormJava7(env0,false) + "\n  }\n";   
    } 

    // System.out.println("JAVA 7 OPERATION CODE: "); 
      
    String resT = "void";
    if (post == null) 
    { return ""; }   // or use the SM if one exists
    if (resultType != null)
    { resT = resultType.getJava7(elementType); }

    if (ent != null && ent.isInterface())
    { return "  public " + resT + " " + name + "(" + pars + ");\n"; } 

    if (query)
    { return buildQueryOpJava7(ent,name,resultType,resT,entities,types); }
    else 
    { return buildUpdateOpJava7(ent,name,resultType,resT,entities,types); }
  } // ignore return type for update ops for now. 

  public String getOperationCodeCSharp(Entity ent, Vector entities, Vector types)
  { String name = getName();
    Vector context = new Vector(); 
    String ename = ""; 
    if (ent != null) 
    { context.add(ent);  
      ename = ent.getName();
    }  
    String ex = ename.toLowerCase() + "x"; 
    java.util.Map env0 = new java.util.HashMap(); 
    String pars = getCSharpParameterDec(); 

    String textcode = ""; 
    if (hasText())
    { textcode = text + "\n"; } 

    /* if ("run".equals(name) && ent.isActive())
    { if (ent != null && ent.isInterface())
      { return "  public void run();\n"; } 

      String res = "  public void run()\n  { "; 
      if (sm != null) 
      { Statement rc = sm.runCode(); 
        // add run_state : T to ent, and the type. 
        rc.typeCheck(types,entities,context,new Vector()); 
        return res + rc.toStringJava() + "  }\n\n"; 
      } 
      return res + " }\n\n"; 
    }  */ 


    if (sm != null) 
    { Statement cde = sm.getSequentialCode(); // sequential code
      if (cde != null) 
      { Vector localatts = new Vector(); 
        localatts.addAll(sm.getAttributes()); 
        String res = ""; 
        Type tp = getResultType(); 
        String ts = "void";
        if (tp != null) 
        { ts = tp.getCSharp(); } 

        String statc = ""; 
        if (ent == null || isClassScope() || isStatic()) 
        { statc = "static "; } 

        res = "  public " + statc + ts + " " + name + "(" + pars + ")\n{  "; 

        if (ent != null && ent.isInterface())
        { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 
 
        if (tp != null)
        { res = res + ts + " result;\n"; }
        localatts.addAll(parameters);   
        cde.typeCheck(types,entities,context,localatts); 
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + cde.updateFormCSharp(env0,true) + "\n  }";
      }   
    } 
    else 
    if (activity != null) 
    { Vector localatts = new Vector(); 
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { env0.put(ename,ex); } //  or "this" if not static ??
      String res = ""; 

      Type tp = getResultType(); 
      String ts = "void";

      String header = ""; 

      if (tp != null) 
      { ts = tp.getCSharp(); } 

      if (ent != null && ent.isInterface())
      { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 

      // if (isSequential())
      // { header = header + "synchronized "; } 
      if (isAbstract())
      { header = header + "abstract "; } 
      if (isFinal())
      { header = header + "sealed "; } 
      if (isClassScope() || isStatic())
      { header = header + "static "; } 

      res = "  public " + header + ts + " " + name + "(" + pars + ")\n  {  "; 

      if (tp != null)
      { res = res + ts + " result;\n"; }
      localatts.addAll(parameters);   
      Vector preterms = activity.allPreTerms(); 
      if (preterms.size() > 0) 
      { Statement newpost = (Statement) activity.clone(); 
        // System.out.println("Pre terms: " + preterms); 
        Vector processed = new Vector(); 
        String newdecs = ""; 
        for (int i = 0; i < preterms.size(); i++)
        { BasicExpression preterm = (BasicExpression) preterms.get(i);
          if (processed.contains(preterm)) { continue; }  
          Type typ = preterm.getType();  // but actual type may be list if multiple
          Type actualtyp; 
          String newdec = ""; 
          String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
          String pretermqf = preterm.classqueryFormCSharp(env0,true); 
          BasicExpression prebe = new BasicExpression(pre_var); 

          if (preterm.isMultiple())
          { if (preterm.isOrdered())
            { actualtyp = new Type("Sequence",null); } 
            else 
            { actualtyp = new Type("Set",null); } 
            actualtyp.setElementType(preterm.getElementType()); 
            newdec = actualtyp.getCSharp() + " " + pre_var + " = new ArrayList();\n" + 
                     "    " + pre_var + ".AddRange(" + pretermqf + ");\n"; 
          } 
          else 
          { actualtyp = typ;
            newdec = actualtyp.getCSharp() + " " + pre_var + " = " + pretermqf + ";\n";
          } 
          newdecs = newdecs + "    " + newdec; 
          prebe.type = actualtyp; 
          prebe.elementType = preterm.elementType; 
          // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
          //                    " elemtype= " + prebe.elementType); 
          
          Attribute preatt = 
            new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
          preatt.setElementType(preterm.elementType); 
          localatts.add(preatt); 
          newpost = newpost.substituteEq("" + preterm,prebe); 
          processed.add(preterm); 
        }  // substitute(preterm,prebe) more appropriate 

        newpost.typeCheck(types,entities,context,localatts);
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + " " + ex + " = this;\n  "; }   
        return res + newdecs + newpost.updateFormCSharp(env0,false) + "\n  }\n";   
      }     // updateForm(, true)? 
      activity.typeCheck(types,entities,context,localatts);
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { res = res + ename + " " + ex + " = this;\n  "; }   
      return res + activity.updateFormCSharp(env0,false) + "\n  }\n";   
    } 
      
    String resT = "void";
    if (post == null) 
    { return ""; }   // or use the SM if one exists
    if (resultType != null)
    { resT = resultType.getCSharp(); }

    if (ent != null && ent.isInterface())
    { return "  public " + resT + " " + name + "(" + pars + ");\n"; } 

    if (query)
    { return buildQueryOpCSharp(ent,name,resultType,resT,entities,types); }
    else 
    { return buildUpdateOpCSharp(ent,name,resultType,resT,entities,types); }
  } // ignore return type for update ops for now. 

  // Operation for entity:
  public String getOperationCodeCPP(Entity ent, Vector entities, Vector types, Vector decs)
  { String name = getName();
    Vector context = new Vector();
    String ename = "";  
    if (ent != null) 
    { context.add(ent);  
      ename = ent.getName();
    }  
    String ex = ename.toLowerCase() + "x"; 
    java.util.Map env0 = new java.util.HashMap(); 
    String pars = getCPPParameterDec(); 

    String textcode = ""; 
    if (hasText())
    { textcode = text + "\n"; } 
    

    /* if ("run".equals(name) && ent.isActive())
    { if (ent.isInterface())
      { return "  public void run();\n"; } 

      String res = "  public void run()\n  { "; 
      if (sm != null) 
      { Statement rc = sm.runCode(); 
        // add run_state : T to ent, and the type. 
        rc.typeCheck(types,entities,context,new Vector()); 
        return res + rc.toStringJava() + "  }\n\n"; 
      } 
      return res + " }\n\n"; 
    }  */ 

    String isstatic = "";  
    if (isClassScope() || isStatic())
    { isstatic = "static "; } 

    if (sm != null) 
    { Statement cde = sm.getSequentialCode(); // sequential code
      if (cde != null) 
      { Vector localatts = new Vector(); 
        localatts.addAll(sm.getAttributes()); 
        String res = ""; 
        Type tp = getResultType(); 
        String ts = "void";
        if (tp != null) 
        { ts = tp.getCPP(elementType); } 
        res = "  " + ts + " " + ename + "::" + name + "(" + pars + ")\n  {  "; 
        decs.add("  " + isstatic + ts + " " + name + "(" + pars + ");\n"); 

        // if (ent.isInterface())
        // { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 
 
        if (tp != null)
        { res = res + ts + " result;\n"; }
        localatts.addAll(parameters);   
        cde.typeCheck(types,entities,context,localatts); 
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + "* " + ex + " = this;\n  "; }   
        return res + cde.updateFormCPP(env0,true) + "\n  }";
      }   
    } 
    else
    if (activity != null) 
    { Vector localatts = new Vector(); 
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { env0.put(ename,ex); } //  or "this" if not static ??
      String res = ""; 

      Type tp = getResultType(); 
      String ts = "void";

      String header = ""; 

      Type elemT = getElementType(); 
      if (tp != null) 
      { ts = tp.getCPP(elemT); } 

      String cet = "void*"; 
      if (elemT != null) 
      { cet = elemT.getCPP(elemT.getElementType()); } 

      // if (ent.isInterface())
      // { return "  public " + ts + " " + name + "(" + pars + ");\n"; } 

      // if (isSequential())
      // { header = header + "synchronized "; } 
      // if (isAbstract())
      // { header = header + "abstract "; } 
      // if (isFinal())
      // { header = header + "sealed "; }


      res = "  " + ts + " " + ename + "::" + name + "(" + pars + ")\n  {  "; 
      decs.add("  " + isstatic + ts + " " + name + "(" + pars + ");\n"); 

      if (tp != null)
      { res = res + ts + " result;\n"; }
      localatts.addAll(parameters);   
      Vector preterms = activity.allPreTerms(); 
      if (preterms.size() > 0) 
      { Statement newpost = (Statement) activity.clone(); 
        // System.out.println("Pre terms: " + preterms); 
        Vector processed = new Vector(); 
        String newdecs = ""; 
        for (int i = 0; i < preterms.size(); i++)
        { BasicExpression preterm = (BasicExpression) preterms.get(i);
          if (processed.contains(preterm)) { continue; }  
          Type typ = preterm.getType();  // but actual type may be list if multiple
          Type actualtyp; 
          String newdec = ""; 
          String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
          String pretermqf = preterm.classqueryFormCPP(env0,true); 
          BasicExpression prebe = new BasicExpression(pre_var); 

          if (preterm.isMultiple())
          { Type pelemT = preterm.getElementType(); 
            String pcet = "void*"; 
            if (pelemT != null) 
            { pcet = pelemT.getCPP(pelemT.getElementType()); }
 
            if (preterm.isOrdered())
            { actualtyp = new Type("Sequence",null);  
              newdec = "vector<" + pcet + ">* " + pre_var + " = new vector<" + pcet + ">();\n" + 
                     "    " + pre_var + "->insert(" + pre_var + "->end(), " + 
                                                      pretermqf + "->begin(), " + 
                                                      pretermqf + "->end());\n";
            }  
            else 
            { actualtyp = new Type("Set",null);  
              newdec = "set<" + pcet + ">* " + pre_var + " = new set<" + pcet + ">();\n" + 
                     "    " + pre_var + "->insert(" + pretermqf + "->begin(), " + 
                                                      pretermqf + "->end());\n";
            }  
            actualtyp.setElementType(pelemT); 
          } 
          else 
          { actualtyp = typ;
            newdec = actualtyp.getCPP() + " " + pre_var + " = " + pretermqf + ";\n";
          } 
          newdecs = newdecs + "    " + newdec; 
          prebe.type = actualtyp; 
          prebe.elementType = preterm.elementType; 
          // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
          //                    " elemtype= " + prebe.elementType); 
          
          Attribute preatt = 
            new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
          preatt.setElementType(preterm.elementType); 
          localatts.add(preatt); 
          newpost = newpost.substituteEq("" + preterm,prebe); 
          processed.add(preterm); 
        }  // substitute(preterm,prebe) more appropriate 

        newpost.typeCheck(types,entities,context,localatts);
        if (ent == null || isClassScope() || isStatic()) { } 
        else 
        { res = res + ename + "* " + ex + " = this;\n  "; }   
        return res + newdecs + newpost.updateFormCPP(env0,false) + "\n  }\n";   
      }     // updateForm(, true)? 
      activity.typeCheck(types,entities,context,localatts);
      if (ent == null || isClassScope() || isStatic()) { } 
      else 
      { res = res + ename + "* " + ex + " = this;\n  "; }   
      return res + activity.updateFormCPP(env0,false) + "\n  }\n";   
    } 
      
    String resT = "void";
    if (post == null) 
    { return ""; }   // or use the SM if one exists
    if (resultType != null)
    { resT = resultType.getCPP(getElementType()); }

    // if (ent.isInterface())
    // { return "  public " + resT + " " + name + "(" + pars + ");\n"; } 

    if (query)
    { return buildQueryOpCPP(ent,name,resultType,resT,entities,types,decs); }
    else 
    { return buildUpdateOpCPP(ent,name,resultType,resT,entities,types,decs); }
  } // ignore return type for update ops for now. 

  public String buildQueryOp(Entity ent, String opname,
                             Type t, String resT, 
                             Vector entities, Vector types)
  { if (resultType == null) 
    { System.err.println("ERROR: No result type for " + opname); 
      return ""; 
    }
    
    /* Scope scope = post.resultScope();
    BinaryExpression rscope = null; 
    if (scope == null) // no range, must assign result
    { System.err.println("WARNING: No scope for result of " + opname); }
    else 
    { rscope = scope.resultScope; }  */ 
 
    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } // But for static should only find static features

    String preheader = ""; 
    
    String javaPars = getJavaParameterDec(); 
    String header = "  public "; 
    if (isSequential())
    { header = header + "synchronized "; } 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 

    String statc = ""; 

    if (isClassScope() || isStatic())
    { header = header + "static "; 
      statc = "static"; 
    } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();
    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 
    // This should not carry over to inner static scopes, eg., in a ->select

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL);
    resultatt.setElementType(elementType);  
    atts.add(resultatt); 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.preconditionExpression();
        preheader = "    if (" + npre.queryForm(env0,true) +
                    ") { return result; } \n  "; 
      }
    } 

    // Expression q0 = post.removeExpression(rscope);
    // Expression q = null;
    // if (q0 != null)
    // { q = q0.substituteEq("result",rxbe); } 

    header = header + "  " + resT + " result = " + resultType.getDefault() + ";\n";
    header = header + preheader; 

    String querycases = buildQueryCases(post,"",resT,env0,types,entities,atts); 

    if (ent != null && isCached() && parameters.size() == 0 && instanceScope) 
    { ent.addAux("  private static java.util.Map " + opname + "_cache = new java.util.HashMap();\n");
      String wresult = Expression.wrap(resultType, "result"); 
      return header + 
             "  Object _cached_result = " + opname + "_cache.get(this);\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = " + Expression.unwrap("_cached_result", resultType) + "; \n" + 
             "    return result; \n" + 
             "  }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache.put(this, " + wresult + ");\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           

    if (ent != null && isCached() && parameters.size() >= 1) 
    { ent.addAux("  private " + statc + " java.util.Map " + opname + "_cache = new java.util.HashMap();\n");
      // Attribute par = (Attribute) parameters.get(0);  
      // String par1 = par.getName();
      String wpar1 = parcats(); // Expression.wrap(par.getType(), par1);  
      String wresult = Expression.wrap(resultType, "result"); 
      return header + 
             "  Object _cached_result = " + opname + "_cache.get(" + wpar1 + ");\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = " + Expression.unwrap("_cached_result", resultType) + "; \n" + 
             "    return result; \n" + 
             "  }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache.put(" + wpar1 + ", " + wresult + ");\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           
 

    return  header + " \n" + querycases + "    return result;\n  }\n"; 
      
    /* if (isEqScope(rscope))
    { rscope.typeCheck(types,entities,atts);  // not resultx though 
      Expression test = null; 
      // if (q instanceof BinaryExpression)
      // { BinaryExpression qbe = (BinaryExpression) q; 
      //   if (qbe.operator.equals("=>"))
      //   { test = qbe.left; }
      // }
      String body = Association.genEventCode(env0,test,rscope,true); 
      return header + "  " + body + "\n" + 
             "    return result;\n  }";
    }  
    if (isInScope(rscope))
    { header = header + " " + resT + " result;\n";
      header = header + preheader; 
      header = header + "    Vector " + rs + " = " +
               getRange(rscope,env0) + ";\n";
      header = header + "    for (int i = 0; i < " +
               rs + ".size(); i++)\n";
      header = header + "    { " + resT + " " + rx +
               " = (" + resT + ") " + rs + ".get(i);\n";
      Expression newpost =
        new BinaryExpression("=",
                    new BasicExpression("result"),rxbe);
      newpost.typeCheck(types,entities,context,atts); 
      if (q != null) 
      { q.typeCheck(types,entities,context,atts); } 
      String body = Association.genEventCode(env0,q,
                                             newpost,true);
      return header + "  " + body + " }\n" + 
             "    return result;\n  }";
    }
    if (isSubScope(rscope))
    { Type elemT = getElementType(rscope);
      if (elemT == null) { return ""; } 
      String jelemT = elemT.getJava(); 
      header = header + "  " + resT + " result = new Vector();\n";
      header = header + preheader; 
      header = header + "    Vector " + rs + " = " +
               getRange(rscope,env0) + ";\n";
      header = header + "    for (int i = 0; i < " +
               rs + ".size(); i++)\n";
      header = header + "    { " + jelemT + " " + rx +
               " = (" + jelemT + ") " + rs + ".get(i);\n";
      Expression newpost =
        new BinaryExpression(":",rxbe,new BasicExpression("result"));
       
      newpost.typeCheck(types,entities,context,atts); 
      if (q != null) 
      { q.typeCheck(types,entities,context,atts); } 
      String body = Association.genEventCode(env0,q,
                                             newpost,true);
      return header + "  " + body + " }\n" + 
             "    return result;\n  }";
    }
    return ""; */ 
  }

  public String buildQueryOpJava6(Entity ent, String opname,
                             Type t, String resT, 
                             Vector entities, Vector types)
  { if (resultType == null) 
    { System.err.println("ERROR: No result type for " + opname); 
      return ""; 
    }
    
    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } // But for static should only find static features

    String preheader = ""; 
    
    // String rx = "resultx";
    // Expression rxbe = new BasicExpression(rx);
    String javaPars = getJava6ParameterDec(); 
    String header = "  public "; 
    if (isSequential())
    { header = header + "synchronized "; } 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 

    String statc = ""; 

    if (isClassScope() || isStatic())
    { header = header + "static "; 
      statc = "static"; 
    } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();
    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL);
    resultatt.setElementType(elementType);  
    // Attribute resultxatt = new Attribute(rx,t,ModelElement.INTERNAL); 
    // resultxatt.setElementType(elementType);  
    atts.add(resultatt); 
    // atts.add(resultxatt); 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.preconditionExpression();
        preheader = "    if (" + npre.queryFormJava6(env0,true) +
                    ") { return result; } \n  "; 
      }
    } 

    // Expression q0 = post.removeExpression(rscope);
    // Expression q = null;
    // if (q0 != null)
    // { q = q0.substituteEq("result",rxbe); } 

    header = header + "  " + resT + " result = " + resultType.getDefaultJava6() + ";\n";
    header = header + preheader; 

    String querycases = buildQueryCasesJava6(post,"",resT,env0,types,entities,atts); 
      
    if (ent != null && isCached() && parameters.size() == 0 && instanceScope) 
    { ent.addAux("  private static java.util.Map " + opname + "_cache = new java.util.HashMap();\n");
      String wresult = Expression.wrap(resultType, "result"); 
      return header + 
             "  Object _cached_result = " + opname + "_cache.get(this);\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = " + Expression.unwrap("_cached_result", resultType) + "; }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache.put(this, " + wresult + ");\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           

    if (ent != null && isCached() && parameters.size() >= 1) 
    { ent.addAux("  private " + statc + " java.util.Map " + opname + "_cache = new java.util.HashMap();\n");
      // Attribute par = (Attribute) parameters.get(0);  
      // String par1 = par.getName();
      String wpar1 = parcats(); // Expression.wrap(par.getType(), par1);  
      String wresult = Expression.wrap(resultType, "result"); 
      return header + 
             "  Object _cached_result = " + opname + "_cache.get(" + wpar1 + ");\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = " + Expression.unwrap("_cached_result", resultType) + "; }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache.put(" + wpar1 + ", " + wresult + ");\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           
 
    return header + "  \n" + querycases + "  \n" + 
           "    return result;\n  }\n";       
  }

  public String buildQueryOpJava7(Entity ent, String opname,
                             Type t, String resT, 
                             Vector entities, Vector types)
  { 
    if (resultType == null) 
    { System.err.println("ERROR: No result type for " + opname); 
      return ""; 
    }
    
    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } // But for static should only find static features

    String preheader = ""; 
    
    // String rx = "resultx";
    // Expression rxbe = new BasicExpression(rx);
    String javaPars = getJava7ParameterDec(); 
    String header = "  public "; 
    if (isSequential())
    { header = header + "synchronized "; } 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 

    String statc = ""; 

    if (isClassScope() || isStatic())
    { header = header + "static "; 
      statc = "static"; 
    } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();
    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL);
    resultatt.setElementType(elementType);  
    // Attribute resultxatt = new Attribute(rx,t,ModelElement.INTERNAL); 
    // resultxatt.setElementType(elementType);  
    atts.add(resultatt); 
    // atts.add(resultxatt); 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.preconditionExpression();
        preheader = "    if (" + npre.queryFormJava7(env0,true) +
                    ") { return result; } \n  "; 
      }
    } 

    header = header + "  " + resT + " result = " + resultType.getDefaultJava7() + ";\n";
    header = header + preheader; 

    String querycases = buildQueryCasesJava7(post,"",resT,env0,types,entities,atts); 
      
    if (ent != null && isCached() && parameters.size() == 0 && instanceScope) 
    { ent.addAux("  private static java.util.Map " + opname + "_cache = new java.util.HashMap();\n");
      String wresult = Expression.wrap(resultType, "result"); 
      return header + 
             "  Object _cached_result = " + opname + "_cache.get(this);\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = " + Expression.unwrap("_cached_result", resultType) + "; }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache.put(this, " + wresult + ");\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           

    if (ent != null && isCached() && parameters.size() >= 1) 
    { ent.addAux("  private " + statc + " java.util.Map " + opname + "_cache = new java.util.HashMap();\n");
      // Attribute par = (Attribute) parameters.get(0);  
      // String par1 = par.getName();
      String wpar1 = parcats(); // Expression.wrap(par.getType(), par1);  
      String wresult = Expression.wrap(resultType, "result"); 
      return header + 
             "  Object _cached_result = " + opname + "_cache.get(" + wpar1 + ");\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = " + Expression.unwrap("_cached_result", resultType) + "; }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache.put(" + wpar1 + ", " + wresult + ");\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           
 
    return header + "  \n" + querycases + "  \n" + 
           "    return result;\n  }\n";       
  }

  public String buildQueryOpCSharp(Entity ent, String opname,
                             Type t, String resT, 
                             Vector entities, Vector types)
  { if (resultType == null) 
    { System.err.println("ERROR: No result type for " + opname); 
      return ""; 
    }
    
    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } // But for static should only find static features

    String preheader = ""; 
    
    // String rx = "resultx";
    // Expression rxbe = new BasicExpression(rx);
    String javaPars = getCSharpParameterDec(); 
    String header = "  public "; 
    // if (isSequential())
    // { header = header + "synchronized "; } 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "sealed "; } 
    String statc = ""; 

    if (isClassScope() || isStatic())
    { header = header + "static "; 
      statc = "static"; 
    } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();
    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL);
    resultatt.setElementType(elementType);  
    // Attribute resultxatt = new Attribute(rx,t,ModelElement.INTERNAL); 
    // resultxatt.setElementType(elementType);  
    atts.add(resultatt); 
    // atts.add(resultxatt); 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.preconditionExpression();
        preheader = "    if (" + npre.queryFormCSharp(env0,true) +
                    ") { return result; } \n  "; 
      }
    } 

    // Expression q0 = post.removeExpression(rscope);
    // Expression q = null;
    // if (q0 != null)
    // { q = q0.substituteEq("result",rxbe); } 

    header = header + "  " + resT + " result = " + resultType.getDefaultCSharp() + ";\n";
    header = header + preheader; 

    String querycases = buildQueryCasesCSharp(post,"",resT,env0,types,entities,atts); 
      
    if (ent != null && isCached() && parameters.size() == 0 && instanceScope) 
    { ent.addAux("  private static Hashtable " + opname + "_cache = new Hashtable();\n");
      String wresult = "result"; // Expression.wrap(resultType, "result"); 
      String tcsh = resultType.getCSharp(); 
      return header + 
             "  object _cached_result = " + opname + "_cache[this];\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = (" + tcsh + ") _cached_result; }\n" +
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache[this] = " + wresult + ";\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           

    if (ent != null && isCached() && parameters.size() >= 1) 
    { ent.addAux("  private " + statc + " Hashtable " + opname + "_cache = new Hashtable();\n");
      // Attribute par = (Attribute) parameters.get(0);  
      // String par1 = par.getName();
      String wpar1 = parcatsCSharp(); // par1; // Expression.wrap(par.getType(), par1);  
      String wresult = "result"; // Expression.wrap(resultType, "result"); 
      String tcsh = resultType.getCSharp(); 
      return header + 
             "  object _cached_result = " + opname + "_cache[" + wpar1 + "];\n" + 
             "  if (_cached_result != null)\n" + 
             "  { result = (" + tcsh + ") _cached_result; }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache[" + wpar1 + "] = " + wresult + ";\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           

    return header + "  \n  " + querycases + " \n" + 
           "    return result;\n  }\n";       
  }

  public String buildQueryOpCPP(Entity ent, String opname,
                             Type t, String resT, 
                             Vector entities, Vector types, Vector decs)
  { if (resultType == null) 
    { System.err.println("ERROR: No result type for " + opname); 
      return ""; 
    }
    

    String ename = ""; 
 
    Vector context = new Vector(); 
    if (ent != null) 
    { ename = ent.getName(); 
      context.add(ent); 
    } // But for static should only find static features

    String preheader = ""; 
    
    // String rx = "resultx";
    // Expression rxbe = new BasicExpression(rx);
    String javaPars = getCPPParameterDec(); 
    String header = "  "; 
    // if (isSequential())
    // { header = header + "synchronized "; } 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    // if (isFinal())
    // { header = header + "sealed "; } 

    String isstatic = ""; 
    if (isClassScope() || isStatic())
    { isstatic = "static "; } 

    // if (isAbstract())
    // { header = header + resT + " " +
    //                 opname + "(" + javaPars + ");\n\n";
    //   return header; 
    // }

    header = "  " + resT + " " + ename + "::" + 
                    opname + "(" + javaPars + ")\n  { ";
    decs.add("  " + isstatic + resT + " " + opname + "(" + javaPars + ");\n"); 

    java.util.Map env0 = new java.util.HashMap();
    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ename,"this"); } 

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL);
    resultatt.setElementType(elementType);  
    // Attribute resultxatt = new Attribute(rx,t,ModelElement.INTERNAL); 
    // resultxatt.setElementType(elementType);  
    atts.add(resultatt); 
    // atts.add(resultxatt); 

    String et = "void*"; 
    if (elementType != null) 
    { et = elementType.getCPP(elementType.getElementType()); } 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.preconditionExpression();
        preheader = "    if (" + npre.queryFormCPP(env0,true) +
                    ") { return result; } \n  "; 
      }
    } 

    // Expression q0 = post.removeExpression(rscope);
    // Expression q = null;
    // if (q0 != null)
    // { q = q0.substituteEq("result",rxbe); } 

    header = header + "  " + resT + " result = " + resultType.getDefaultCPP(elementType) + ";\n";
    header = header + preheader; 


    String querycases = buildQueryCasesCPP(post,"",resT,env0,types,entities,atts); 

    if (ent != null && isCached() && parameters.size() == 0 && instanceScope) 
    { String etype = "void*"; 
      if (elementType != null) 
      { etype = elementType.getCPP(); } 
      String tcsh = resultType.getCPP(etype); 

      ent.addAux("  static map<" + ename + "*," + tcsh + "> " + opname + "_cache;\n");
      ent.addPrivateAux("  map<" + ename + "*," + tcsh + "> " + ename + "::" + opname + "_cache;\n");

      String wresult = "result"; // Expression.wrap(resultType, "result"); 
      return header + 
             "  map<" + ename + "*," + tcsh + ">::iterator _cached_pos = " + opname + "_cache.find(this);\n" + 
             "  if (_cached_pos != " + opname + "_cache.end())\n" + 
             "  { result = " + opname + "_cache[this]; }\n" +
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    (" + opname + "_cache*)[this] = " + wresult + ";\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           

    /* General caching for more than 1 parameter is not possible in C++ */ 

    if (ent != null && isCached() && parameters.size() == 1 && instanceScope) 
    { String etype = "void*"; 
      if (elementType != null) 
      { etype = elementType.getCPP(elementType.getElementType()); } 
      String tcsh = resultType.getCPP(etype); 
      Attribute par = (Attribute) parameters.get(0);  
      String par1 = par.getName();
      String ptyp = par.getType().getCPP();  // must be primitive or String
      // String wpar1 = parcatsCSharp();   
      ent.addAux("  map<" + ptyp + "," + tcsh + "> " + opname + "_cache;\n");
      String wresult = "result";  
      return header + 
             "  map<" + ptyp + "," + tcsh + ">::iterator _cached_pos = " + opname + "_cache.find(" + par1 + ");\n" + 
             "  if (_cached_pos != " + opname + "_cache.end())\n" + 
             "  { result = " + opname + "_cache[" + par1 + "]; }\n" + 
             "  else \n" + 
             "  { " + querycases + "\n" + 
             "    " + opname + "_cache[" + par1 + "] = " + wresult + ";\n" + 
             "  }\n" + 
             "  return result;\n" + 
             " }\n"; 
    }           

    return header + querycases + " \n" + 
           "    return result;\n  }\n";       

  }

  public Statement designQueryList(Vector postconds, String resT,
                                 java.util.Map env0, 
                                 Vector types, Vector entities, Vector atts)
  { if (postconds.size() == 0)
    { return new SequenceStatement(); } 

    Expression postcond = (Expression) postconds.get(0); 
    Statement fst = designBasicCase(postcond,resT,
                                     env0,types,entities,atts);
    if (postconds.size() == 1) 
    { return fst; } 

    Vector ptail = new Vector(); 
    ptail.addAll(postconds); 
    ptail.remove(0); 
 
    if ((postcond instanceof BinaryExpression) &&  
            "=>".equals(((BinaryExpression) postcond).operator))
    { Expression next = (Expression) postconds.get(1); 

      if ((next instanceof BinaryExpression) &&
              "=>".equals(((BinaryExpression) next).operator)) 
      { Statement elsepart = designQueryList(ptail,resT,
                                             env0,types,entities,atts);
        ((IfStatement) fst).addCases((IfStatement) elsepart);
           // Statement.combineIfStatements(fst,elsepart);
        return fst;   
      } 
      else 
      { Statement stat = designQueryList(ptail,resT,
                                           env0,types,entities,atts); 
        SequenceStatement res = new SequenceStatement(); 
        res.addStatement(fst); res.addStatement(stat); 
        return res; 
      } 
    }      
    else 
    { Statement stat = designQueryList(ptail,resT,
                                           env0,types,entities,atts); 
      SequenceStatement res = new SequenceStatement(); 
      res.addStatement(fst); res.addStatement(stat); 
      return res;
    }  // sequencing of fst and ptail
  } 

  /* if (postcond instanceof BinaryExpression)
    { BinaryExpression pst = (BinaryExpression) postcond;
      if ("&".equals(pst.operator))
      { Statement fst = designQueryCases(pst.left, resT, env0, types, entities, atts);
        Statement scnd = designQueryCases(pst.right, resT, env0, types, entities, atts);
        if ((pst.left instanceof BinaryExpression) && "=>".equals(((BinaryExpression) pst.left).operator))
        { IfStatement res = new IfStatement(fst, scnd); 
          return res;
        }
        else
        { SequenceStatement ss = new SequenceStatement();
          ss.addStatement(fst);
          ss.addStatement(scnd);
          return ss;
        }
      }
      else if ("=>".equals(pst.operator))
      { Statement body = designQueryCases(pst.right, resT, env0, types, entities, atts);
        IfStatement res = new IfStatement(pst.left, body);
        return res;
      }
    }
    return designBasicCase(postcond, resT, env0, types, entities, atts); 
  } */

  private Statement designBasicCase(Expression pst,String resT, java.util.Map env0,
                                Vector types, Vector entities, Vector atts)
  { if (pst instanceof BinaryExpression) 
    { BinaryExpression be = (BinaryExpression) pst; 
      if ("=>".equals(be.operator))
      { Expression test = be.left; 

        Constraint virtualCon = new Constraint(test,be.right); 

        Vector pars = new Vector(); 
        pars.addAll(getParameters()); 
        Vector lvars = new Vector(); 
        virtualCon.secondaryVariables(lvars,pars); 

        Vector allvars = new Vector(); 
        Vector allpreds = new Vector(); 
        Vector qvars1 = new Vector(); 
        Vector lvars1 = new Vector(); 
        Vector pars1 = ModelElement.getNames(pars); 
        Vector v0 = new Vector(); 
        BasicExpression betrue = new BasicExpression(true); 
        v0.add(betrue); 
        v0.add(betrue.clone()); 
        Vector splitante = test.splitToCond0Cond1Pred(v0,pars1,qvars1,lvars1,allvars,allpreds); 
        System.out.println(">>> Quantified local = " + qvars1 + " Let local = " + lvars1 + " All: " + allvars); 
       
        Expression ante1 = (Expression) splitante.get(0); 
        Expression ante2 = (Expression) splitante.get(1); 

        System.out.println(">>> Variable quantifiers: " + ante1); 
        System.out.println(">>> Assumptions: " + ante2);

        Statement ifpart = // new ImplicitInvocationStatement(be.right); 
                           designBasicCase(be.right, resT, env0, types, entities, atts); 
        if (allvars.size() > 0) 
        { Statement forloop = virtualCon.q2LoopsPred(allvars,qvars1,lvars1,ifpart); 
          return forloop; 
        } 
        return new IfStatement(test, ifpart); 
      } // But may be let definitions and local variables in test. 
      else if ("=".equals(be.operator))
      { Expression beleft = be.left; 
        if (env0.containsValue(be.left + "") || "result".equals(be.left + "") ||
            ModelElement.getNames(parameters).contains(be.left + ""))
        { return new AssignStatement(be.left, be.right); } // or attribute of ent
        else if (entity != null && entity.hasFeature(be.left + "")) 
        { return new AssignStatement(be.left, be.right); }
        else // declare it
        { Type t = be.left.getType(); 
          JOptionPane.showMessageDialog(null, 
            "Declaring new local variable " + be.left + " in:\n" + this,               
            "Implicit variable declaration", JOptionPane.INFORMATION_MESSAGE); 
          CreationStatement cs = new CreationStatement(t.getJava(), be.left + ""); 
          cs.setInstanceType(t); 
          cs.setElementType(t.getElementType()); 
          AssignStatement ast = new AssignStatement(be.left, be.right);
          SequenceStatement sst = new SequenceStatement(); 
          sst.addStatement(cs); sst.addStatement(ast); 
          return sst;  
        } 
      } 
    }
    return pst.generateDesign(env0, true);
  }

  // assume it is a conjunction of implications
  private String buildQueryList(Vector postconds, String header,String resT,
                                 java.util.Map env0, 
                                 Vector types, Vector entities, Vector atts)
  { if (postconds.size() == 0)
    { return ""; } 

    Expression postcond = (Expression) postconds.get(0); 
    String fst = buildQueryCases(postcond,header,resT,
                                     env0,types,entities,atts) + " ";
    if (postconds.size() == 1) 
    { return fst; } 
    Vector ptail = new Vector(); 
    ptail.addAll(postconds); 
    ptail.remove(0); 
 
    if ((postcond instanceof BinaryExpression) &&  
            "=>".equals(((BinaryExpression) postcond).operator))
    { fst = fst + " else\n    ";  
      Expression next = (Expression) postconds.get(1); 

      if ((next instanceof BinaryExpression) &&
              "=>".equals(((BinaryExpression) next).operator)) 
      { return buildQueryList(ptail,fst,resT,
                                           env0,types,entities,atts) + "   "; 
      } 
      else 
      { fst = fst + "  { "; 
            return buildQueryList(ptail,fst,resT,
                                           env0,types,entities,atts) + "\n   } "; 
      } 
    }      
    else 
    { return buildQueryList(ptail,fst,resT,
                                           env0,types,entities,atts) + "   ";
    }  // sequencing of fst and ptail
  } 

  // assume it is a conjunction of implications
  private String buildQueryCases(Expression postcond, String header,String resT,
                                 java.util.Map env0, 
                                 Vector types, Vector entities, Vector atts)
  { if (postcond instanceof BinaryExpression)
    { BinaryExpression pst = (BinaryExpression) postcond; 
      if ("&".equals(pst.operator))  // and left right both have => as top operator
      { String fst = buildQueryCases(pst.left,header,resT,
                                     env0,types,entities,atts) + " "; 
        if ((pst.left instanceof BinaryExpression) &&  
            "=>".equals(((BinaryExpression) pst.left).operator))
        { fst = fst + " else\n    ";  
          if ((pst.right instanceof BinaryExpression) &&
              "=>".equals(((BinaryExpression) pst.right).operator)) 
          { return buildQueryCases(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
          } 
          else 
          { fst = fst + "  { "; 
            return buildQueryCases(pst.right,fst,resT,
                                           env0,types,entities,atts) + "\n   } "; 
          } 
        } 
        else 
        { return buildQueryCases(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
        }
      } 

      Vector context = new Vector(); 
      if (entity != null) 
      { context.add(entity); } 

      if ("=>".equals(pst.operator))
      { Expression test = pst.left; 
        test.typeCheck(types,entities,context,atts); 
        String qf = test.queryForm(env0,true); 
        // System.out.println(test + " QUERY FORM= " + qf); 

        Constraint virtualCon = new Constraint(test,pst.right); 

        Vector pars = new Vector(); 
        pars.addAll(getParameters()); 
        Vector lvars = new Vector(); 
        virtualCon.secondaryVariables(lvars,pars); 

        Vector allvars = new Vector(); 
        Vector allpreds = new Vector(); 

        Vector qvars1 = new Vector(); 
        Vector lvars1 = new Vector(); 
        Vector pars1 = ModelElement.getNames(pars); 
        Vector v0 = new Vector(); 
        BasicExpression betrue = new BasicExpression(true); 
        v0.add(betrue); 
        v0.add(betrue.clone()); 
        Vector splitante = test.splitToCond0Cond1Pred(v0,pars1,qvars1,lvars1,allvars,allpreds); 
        System.out.println(); 
        System.out.println(">>> Operation " + getName() + " Quantified local = " + 
                           qvars1 + " Let local = " + lvars1 + " All: " + allvars); 
       
        Expression ante1 = (Expression) splitante.get(0); 
        Expression ante2 = (Expression) splitante.get(1); 

        System.out.println(">>> Operation " + getName() + " Variable quantifiers: " + ante1); 
        System.out.println(">>> Operation " + getName() + " Assumptions: " + ante2);
        System.out.println(); 

        if (allvars.size() > 0) 
        { Statement ifpart = new ImplicitInvocationStatement(pst.right);
             // designBasicCase(pst.right, resT, env0, types, entities, atts); 
          Statement forloop = virtualCon.q2LoopsPred(allvars,qvars1,lvars1,ifpart); 
          // System.out.println(">>> Actual code= " + forloop); 
          return forloop.updateForm(env0,true,types,entities,atts); 
        } 
        
        header = header + "  if (" + qf + ") \n  { "; 
        String body = buildQueryCases(pst.right,header,resT,
                                      env0,types,entities,atts); 
        return body + " \n  }"; 
      } 

      if ("or".equals(pst.operator))
      { return buildQueryCases(pst.left,header,resT,env0,types,entities,atts); } 
      else 
      { return header + basicQueryCase(pst,resT,env0,types,entities,atts); } 
    }
    return header; 
  } 

  private String buildQueryCasesJava6(Expression postcond, String header,String resT,
                                 java.util.Map env0, 
                                 Vector types, Vector entities, Vector atts)
  { if (postcond instanceof BinaryExpression)
    { BinaryExpression pst = (BinaryExpression) postcond; 
      if ("&".equals(pst.operator))
      { String fst = buildQueryCasesJava6(pst.left,header,resT,
                                     env0,types,entities,atts) + " "; 
        if ((pst.left instanceof BinaryExpression) &&  
            "=>".equals(((BinaryExpression) pst.left).operator))
        { fst = fst + " else\n    ";  
          if ((pst.right instanceof BinaryExpression) &&
              "=>".equals(((BinaryExpression) pst.right).operator)) 
          { return buildQueryCasesJava6(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
          } 
          else 
          { fst = fst + "  { "; 
            return buildQueryCasesJava6(pst.right,fst,resT,
                                           env0,types,entities,atts) + "\n   } "; 
          } 
        } 
        else 
        { return buildQueryCasesJava6(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
        }
      } 
      Vector context = new Vector(); 
      if (entity != null) 
      { context.add(entity); } 

      if ("=>".equals(pst.operator))
      { Expression test = pst.left; 
        test.typeCheck(types,entities,context,atts); 
        String qf = test.queryFormJava6(env0,true); 
        // System.out.println(test + " QUERY FORM= " + qf); 

        header = header + "  if (" + qf + ") \n  { "; 
        String body = buildQueryCasesJava6(pst.right,header,resT,
                                      env0,types,entities,atts); 
        return body + " \n  }"; 
      } 
      if ("or".equals(pst.operator))
      { return buildQueryCasesJava6(pst.left,header,resT,env0,types,entities,atts); } 
      else 
      { return header + basicQueryCaseJava6(pst,resT,env0,types,entities,atts); } 
    }
    return header; 
  } 

  private String buildQueryCasesJava7(Expression postcond, String header,String resT,
                                 java.util.Map env0, 
                                 Vector types, Vector entities, Vector atts)
  { // System.out.println("JAVA7 QUERY CASES"); 
    if (postcond instanceof BinaryExpression)
    { BinaryExpression pst = (BinaryExpression) postcond; 
      if ("&".equals(pst.operator))
      { String fst = buildQueryCasesJava7(pst.left,header,resT,
                                     env0,types,entities,atts) + " "; 
        if ((pst.left instanceof BinaryExpression) &&  
            "=>".equals(((BinaryExpression) pst.left).operator))
        { fst = fst + " else\n    ";  
          if ((pst.right instanceof BinaryExpression) &&
              "=>".equals(((BinaryExpression) pst.right).operator)) 
          { return buildQueryCasesJava7(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
          } 
          else 
          { fst = fst + "  { "; 
            return buildQueryCasesJava7(pst.right,fst,resT,
                                           env0,types,entities,atts) + "\n   } "; 
          } 
        } 
        else 
        { return buildQueryCasesJava7(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
        }
      } 
      Vector context = new Vector(); 
      if (entity != null) 
      { context.add(entity); } 

      if ("=>".equals(pst.operator))
      { Expression test = pst.left; 
        test.typeCheck(types,entities,context,atts); 
        String qf = test.queryFormJava7(env0,true); 
        // System.out.println(test + " QUERY FORM= " + qf); 

        header = header + "  if (" + qf + ") \n  { "; 
        String body = buildQueryCasesJava7(pst.right,header,resT,
                                      env0,types,entities,atts); 
        return body + " \n  }"; 
      } 
      if ("or".equals(pst.operator))
      { return buildQueryCasesJava7(pst.left,header,resT,env0,types,entities,atts); } 
      else 
      { return header + basicQueryCaseJava7(pst,resT,env0,types,entities,atts); } 
    }
    return header; 
  } 

  private String buildQueryCasesCSharp(Expression postcond, String header, String resT,
                                 java.util.Map env0, 
                                 Vector types, Vector entities, Vector atts)
  { if (postcond instanceof BinaryExpression)
    { BinaryExpression pst = (BinaryExpression) postcond; 
      if ("&".equals(pst.operator))
      { String fst = buildQueryCasesCSharp(pst.left,header,resT,
                                     env0,types,entities,atts) + " "; 
        if ((pst.left instanceof BinaryExpression) &&  
            "=>".equals(((BinaryExpression) pst.left).operator))
        { fst = fst + " else\n    ";  
          if ((pst.right instanceof BinaryExpression) &&
              "=>".equals(((BinaryExpression) pst.right).operator)) 
          { return buildQueryCasesCSharp(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
          } 
          else 
          { fst = fst + "  { "; 
            return buildQueryCasesCSharp(pst.right,fst,resT,
                                           env0,types,entities,atts) + "\n   } "; 
          } 
        } 
        else 
        { return buildQueryCasesCSharp(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
        }
      } 

      Vector context = new Vector(); 
      if (entity != null) 
      { context.add(entity); } 

      if ("=>".equals(pst.operator))
      { Expression test = pst.left; 
        test.typeCheck(types,entities,context,atts); 
        String qf = test.queryFormCSharp(env0,true); 
        // System.out.println(test + " QUERY FORM= " + qf); 

        header = header + "  if (" + qf + ") \n  { "; 
        String body = buildQueryCasesCSharp(pst.right,header,resT,
                                      env0,types,entities,atts); 
        return body + " \n  }"; 
      } 
      if ("or".equals(pst.operator))
      { return buildQueryCasesCSharp(pst.left,header,resT,env0,types,entities,atts); } 
      else 
      { return header + basicQueryCaseCSharp(pst,resT,env0,types,entities,atts); } 
    }
    return header; 
  } 

  private String buildQueryCasesCPP(Expression postcond, String header,String resT,
                                 java.util.Map env0, 
                                 Vector types, Vector entities, Vector atts)
  { if (postcond instanceof BinaryExpression)
    { BinaryExpression pst = (BinaryExpression) postcond; 
      if ("&".equals(pst.operator))
      { String fst = buildQueryCasesCPP(pst.left,header,resT,
                                     env0,types,entities,atts) + " "; 
        if ((pst.left instanceof BinaryExpression) &&  
            "=>".equals(((BinaryExpression) pst.left).operator))
        { fst = fst + " else\n    ";  
          if ((pst.right instanceof BinaryExpression) &&
              "=>".equals(((BinaryExpression) pst.right).operator)) 
          { return buildQueryCasesCPP(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
          } 
          else 
          { fst = fst + "  { "; 
            return buildQueryCasesCPP(pst.right,fst,resT,
                                           env0,types,entities,atts) + "\n   } "; 
          } 
        } 
        else 
        { return buildQueryCasesCPP(pst.right,fst,resT,
                                           env0,types,entities,atts) + "   "; 
        }      
      } 

      Vector context = new Vector(); 
      if (entity != null) 
      { context.add(entity); } 

      if ("=>".equals(pst.operator))
      { Expression test = pst.left; 
        test.typeCheck(types,entities,context,atts); 
        String qf = test.queryFormCPP(env0,true); 
        // System.out.println(test + " QUERY FORM= " + qf); 

        header = header + "  if (" + qf + ") \n  { "; 
        String body = buildQueryCasesCPP(pst.right,header,resT,
                                         env0,types,entities,atts); 
        return body + " \n  }"; 
      } 

      if ("or".equals(pst.operator))
      { return buildQueryCasesCPP(pst.left,header,resT,env0,types,entities,atts); } 
      else 
      { return header + basicQueryCaseCPP(pst,resT,env0,types,entities,atts); } 
    }
    return header; 
  } 



  private String basicQueryCase(Expression pst,String resT, java.util.Map env0,
                                Vector types, Vector entities, Vector atts)
  { // Scope scope = pst.resultScope(); 
   
    // if (scope == null) 
     if (pst instanceof BinaryExpression) 
      { BinaryExpression be = (BinaryExpression) pst; 
        if ("=".equals(be.operator))
        { Expression beleft = be.left; 
          if (env0.containsValue(be.left + "") || "result".equals(be.left + "") || 
              ModelElement.getNames(parameters).contains(be.left + ""))
          { return "  " + pst.updateForm(env0,true) + "\n"; } // or attribute of ent
          else if (entity != null && entity.hasFeature(be.left + "")) 
          { return "  " + pst.updateForm(env0,true) + "\n"; }
          else // declare it
          { Type t = be.right.getType(); 
            System.out.println("Declaring new local variable " + be.left + 
                               " in:\n" + this); 
            // JOptionPane.showMessageDialog(null, 
            //   "Declaring new local variable " + be.left + " in:\n" + this,               
            //   "Implicit variable declaration", JOptionPane.INFORMATION_MESSAGE); 
            return "  " + t.getJava() + " " + pst.updateForm(env0,true) + " \n  "; 
          } 
        }
        else if ("&".equals(be.operator))
        { String fst = basicQueryCase(be.left, resT, env0, types, entities, atts); 
          return fst + basicQueryCase(be.right, resT, env0, types, entities, atts); 
        }  
      } 
      return "  " + pst.updateForm(env0,true) + "\n  ";
     
 
   /* BinaryExpression rscope = scope.resultScope; 
    String op = scope.scopeKind; 
    String rx = "resultx";
    Expression rxbe = new BasicExpression(rx);
    String rs = "results";

    Expression q0 = pst.removeExpression(rscope);
    Expression q = null;
    if (q0 != null)
    { q = q0.substituteEq("result",rxbe); } 
    // What is the point of this??? 

    Vector context = new Vector(); 
    if (entity != null) 
    { context.add(entity); } 

    if (op.equals("eq"))
    { rscope.typeCheck(types,entities,context,atts);  // not resultx though 
      Expression test = null; 

      // if (q instanceof BinaryExpression)
      // { BinaryExpression qbe = (BinaryExpression) q; 
      //   if (qbe.operator.equals("=>"))
      //   { test = qbe.left; }
      // }
      String body = Association.genEventCode(env0,test,rscope,true); 
      return "  " + body + "\n";  
            // + "    return result;\n  ";
    }  
    if (op.equals("in"))
    { String res = "    Vector " + rs + " = new Vector();\n" +
                   "    " + rs + ".addAll(" + getRange(rscope,env0) + ");\n";
      res = res + "    for (int i = 0; i < " + rs + ".size(); i++)\n";
      res = res + "    { " + resT + " " + rx + " = (" + resT + ") " + rs + ".get(i);\n";
      Expression newpost =
        new BinaryExpression("=", new BasicExpression("result"), rxbe);
      newpost.typeCheck(types,entities,context,atts); 
      if (q != null) 
      { q.typeCheck(types,entities,context,atts); } 
      String body = Association.genEventCode(env0,q,
                                             newpost,true);
      return res + "  " + body + " }\n" + 
             "    return result;\n  ";
    }
    if (op.equals("subset"))
    { Type elemT = getElementType(rscope);
      if (elemT == null) { return ""; } 
      String jelemT = elemT.getJava(); 
      header = header + "  " + resT + " result = new Vector();\n";
      header = header + preheader; 
      header = header + "    Vector " + rs + " = " +
               getRange(rscope,env0) + ";\n";
      header = header + "    for (int i = 0; i < " +
               rs + ".size(); i++)\n";
      header = header + "    { " + jelemT + " " + rx +
               " = (" + jelemT + ") " + rs + ".get(i);\n";
      Expression newpost =
        new BinaryExpression(":",rxbe,new BasicExpression("result"));
       
      newpost.typeCheck(types,entities,context,atts); 
      if (q != null) 
      { q.typeCheck(types,entities,context,atts); } 
      String body = Association.genEventCode(env0,q,
                                             newpost,true);
      return header + "  " + body + " }\n" + 
             "    return result;\n  }";
    }  
    if (op.equals("array"))
    { BasicExpression arr = (BasicExpression) scope.arrayExp.clone();
      arr.arrayIndex = null; 
      Expression teste = rscope.substituteEq("result",rxbe); 
      arr.typeCheck(types,entities,context,atts); 
      teste.typeCheck(types,entities,context,atts); 
      String e = arr.queryForm(env0,true); 
      String test = teste.queryForm(env0,true); 
      String loop = 
        "  result = 0;\n" + 
        "  for (int resultx = 1; resultx <= " + e + ".size(); resultx++)\n" + 
        "  { if (" + test + ") \n" + 
        "    { result = resultx; \n" + 
        "      return result; \n" +  
        "    } \n" + 
        "  }\n"; 
      return "  " + loop + "  return result;\n";
    }  
    return ""; */ 
  }

  private String basicQueryCaseJava6(Expression pst,String resT, java.util.Map env0,
                                Vector types, Vector entities, Vector atts)
  { Scope scope = pst.resultScope(); 
   
      if (pst instanceof BinaryExpression) 
      { BinaryExpression be = (BinaryExpression) pst; 
        if ("=".equals(be.operator))
        { Expression beleft = be.left; 
          if (env0.containsValue(be.left + "") || "result".equals(be.left + "") ||
              ModelElement.getNames(parameters).contains(be.left + ""))
          { return "  " + pst.updateFormJava6(env0,true) + "\n"; } // or attribute of ent
          else if (entity != null && entity.hasFeature(be.left + "")) 
          { return "  " + pst.updateFormJava6(env0,true) + "\n"; }
          else // declare it
          { Type t = be.right.getType(); 
            JOptionPane.showMessageDialog(null, 
              "Declaring new local variable " + be.left + " in:\n" + this,               
              "Implicit variable declaration", JOptionPane.INFORMATION_MESSAGE); 
            return "  " + t.getJava6() + " " + pst.updateFormJava6(env0,true) + " \n  "; 
          } 
        } 
      } 
      return "  " + pst.updateFormJava6(env0,true);
  }

  private String basicQueryCaseJava7(Expression pst,String resT, java.util.Map env0,
                                Vector types, Vector entities, Vector atts)
  { if (pst instanceof BinaryExpression) 
    { BinaryExpression be = (BinaryExpression) pst; 
      if ("=".equals(be.operator))
      { Expression beleft = be.left; 
        if (env0.containsValue(be.left + "") || "result".equals(be.left + "") || 
            ModelElement.getNames(parameters).contains(be.left + ""))
        { return "  " + pst.updateFormJava7(env0,true) + "\n"; } // or attribute of ent
        else if (entity != null && entity.hasFeature(be.left + "")) 
        { return "  " + pst.updateFormJava7(env0,true) + "\n"; }
        else // declare it
        { Type t = be.right.getType(); 
          JOptionPane.showMessageDialog(null, 
            "Declaring new local variable " + be.left + " : " + t + " in:\n" + this,               
            "Implicit variable declaration", JOptionPane.INFORMATION_MESSAGE); 
          return "  " + t.getJava7(t.getElementType()) + " " + pst.updateFormJava7(env0,true) + " \n  "; 
        } 
      } 
    } 
    return "  " + pst.updateFormJava7(env0,true);
  }

  private String basicQueryCaseCSharp(Expression pst,String resT, java.util.Map env0,
                                Vector types, Vector entities, Vector atts)
  { if (pst instanceof BinaryExpression) 
    { BinaryExpression be = (BinaryExpression) pst; 
      if ("=".equals(be.operator))
      { Expression beleft = be.left; 
        if (env0.containsValue(be.left + "") || "result".equals(be.left + "") || 
            ModelElement.getNames(parameters).contains(be.left + ""))
        { return "  " + pst.updateFormCSharp(env0,true); } // or attribute of ent
        else if (entity != null && entity.hasFeature(be.left + "")) 
        { return "  " + pst.updateFormCSharp(env0,true); }
        else // declare it
        { Type t = be.left.getType(); 
          JOptionPane.showMessageDialog(null, 
            "Declaring new local variable " + be.left + " in:\n" + this,               
            "Implicit variable declaration", JOptionPane.INFORMATION_MESSAGE); 
          return "  " + t.getCSharp() + " " + pst.updateFormCSharp(env0,true) + " \n  "; 
        } 
      } 
    } 
    return "  " + pst.updateFormCSharp(env0,true); 
  }

  private String basicQueryCaseCPP(Expression pst,String resT, java.util.Map env0,
                                   Vector types, Vector entities, Vector atts)
  { if (pst instanceof BinaryExpression) 
    { BinaryExpression be = (BinaryExpression) pst; 
      if ("=".equals(be.operator))
      { Expression beleft = be.left; 
        if (env0.containsValue(be.left + "") || "result".equals(be.left + "") || 
            ModelElement.getNames(parameters).contains(be.left + ""))
        { return "  " + pst.updateFormCPP(env0,true); } // or attribute of ent
        else if (entity != null && entity.hasFeature(be.left + "")) 
        { return "  " + pst.updateFormCPP(env0,true); }
        else // declare it
        { Type t = be.left.getType(); 
          JOptionPane.showMessageDialog(null, 
            "Declaring new local variable " + be.left + " in:\n" + this,               
            "Implicit variable declaration", JOptionPane.INFORMATION_MESSAGE); 
          return "  " + t.getCPP(t.getElementType()) + " " + pst.updateFormCPP(env0,true) + " \n  "; 
        } 
      } 
    } 
    return "  " + pst.updateFormCPP(env0,true);
  } 


  // Controller version of the operation -- only for instance operations. Should be 
  // for static also? 

  public String getGlobalOperationCode(Entity ent,Vector entities, Vector types,
                                       Vector constraints)
  { String name = getName();
    String resT = "void";
    if (resultType != null)
    { resT = resultType.getJava(); }
    if (post == null)
    { return ""; }
    if (query) 
    { return buildGlobalQueryOp(ent,name,resultType,resT,
                                entities,types); 
    }
    else  // including for abstract ones
    { return buildGlobalUpdateOp(ent,name,resultType,resT,
                                 entities,types,constraints); 
    }
  } // ignore return type for update ops for now. 

  public String getGlobalOperationCodeJava6(Entity ent,Vector entities, Vector types,
                                       Vector constraints)
  { String name = getName();
    String resT = "void";
    if (resultType != null)
    { resT = resultType.getJava6(); }
    if (post == null)
    { return ""; }
    if (query) 
    { return buildGlobalQueryOpJava6(ent,name,resultType,resT,
                                entities,types); 
    }
    else  // including for abstract ones
    { return buildGlobalUpdateOpJava6(ent,name,resultType,resT,
                                 entities,types,constraints); 
    }
  } // ignore return type for update ops for now. 

  public String getGlobalOperationCodeJava7(Entity ent,Vector entities, Vector types,
                                       Vector constraints)
  { String name = getName();
    String resT = "void";
    if (resultType != null)
    { resT = resultType.getJava7(elementType); } // may need the wrapper type down below. 
    if (post == null)
    { return ""; }
    if (query) 
    { return buildGlobalQueryOpJava7(ent,name,resultType,resT,
                                entities,types); 
    }
    else  // including for abstract ones
    { return buildGlobalUpdateOpJava7(ent,name,resultType,resT,
                                 entities,types,constraints); 
    }
  } // ignore return type for update ops for now. 


  // Controller version of the operation
  public String getGlobalOperationCodeCSharp(Entity ent,Vector entities, Vector types,
                                       Vector constraints)
  { // if (!instanceScope) { return ""; } 

    String name = getName();
    String resT = "void";
    if (resultType != null)
    { resT = resultType.getCSharp(); }
    if (post == null)
    { return ""; }
    if (query) 
    { return buildGlobalQueryOpCSharp(ent,name,resultType,resT,
                                entities,types); 
    }
    else  // including for abstract ones
    { return buildGlobalUpdateOpCSharp(ent,name,resultType,resT,
                                 entities,types,constraints); 
    }
  } // ignore return type for update ops for now. 

  // Controller version of the operation
  public String getGlobalOperationCodeCPP(Entity ent,Vector entities, Vector types,
                                       Vector constraints, Vector declarations)
  { if (!instanceScope) { return ""; } 

    String name = getName();
    String resT = "void";
    if (resultType != null)
    { resT = resultType.getCPP(getElementType()); }
    if (post == null)
    { return ""; }
    if (query) 
    { return buildGlobalQueryOpCPP(ent,name,resultType,resT,declarations, 
                                entities,types); 
    }
    else  // including for abstract ones
    { return buildGlobalUpdateOpCPP(ent,name,resultType,resT,declarations, 
                                 entities,types,constraints); 
    }
  } // ignore return type for update ops for now. 


  public String buildUpdateOp(Entity ent, String opname,
                              Type t, String resT, 
                              Vector entities, Vector types)
  { String preheader = ""; 
    String javaPars = getJavaParameterDec(); 
    
    String header = "  public "; 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    if (isClassScope() || isStatic())
    { header = header + "static "; } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();

    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = new Vector(); 
    atts.addAll(parameters); 

    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { header = header + "  " + resT + " result = " + resultType.getDefault() + ";\n"; 
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.computeNegation4succ(); 
        preheader = "  //  if (" + npre.queryForm(env0,true) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 

    Expression newpost = (Expression) post.clone(); 
    Vector preterms = post.allPreTerms(); 
    // System.out.println("Pre terms: " + preterms); 
    // substitute these by new variables pre_var in post, copy types/multiplicities
    // of preterm variables. Have initial assignments pre_var = var.queryForm();
    // for each. Can optimise by ignoring vars not on any lhs

    Vector processed = new Vector(); 

    for (int i = 0; i < preterms.size(); i++)
    { BasicExpression preterm = (BasicExpression) preterms.get(i); 
      if (processed.contains(preterm)) { continue; } 

      Type typ = preterm.getType();  // but actual type may be list if multiple
      Type actualtyp; 
      String newdec = ""; 
      String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
      String pretermqf = preterm.queryForm(env0,true); 
      BasicExpression prebe = new BasicExpression(pre_var); 
      
      if (preterm.isMultiple())
      { if (preterm.isOrdered())
        { actualtyp = new Type("Sequence",null); } 
        else 
        { actualtyp = new Type("Set",null); } 
        actualtyp.setElementType(preterm.getElementType()); 
 
        if (preterm.umlkind == Expression.CLASSID && preterm.arrayIndex == null) 
        { pretermqf = "Controller.inst()." + pretermqf.toLowerCase() + "s"; } 
        newdec = actualtyp.getJava() + " " + pre_var + " = new Vector();\n" + 
                 "    " + pre_var + ".addAll(" + pretermqf + ");\n"; 
      } 
      else 
      { actualtyp = typ;
        newdec = actualtyp.getJava() + " " + pre_var + " = " + pretermqf + ";\n";
      } 
      newdecs = newdecs + "    " + newdec; 
      prebe.type = actualtyp; 
      prebe.elementType = preterm.elementType; 
      Attribute preatt = new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
      // System.out.println("****** PRE variable " + prebe + " type= " + actualtyp + 
      //                    " elemtype= " + prebe.elementType + " VALUE= " + pretermqf); 
      preatt.setElementType(preterm.elementType); 
      atts.add(preatt); 
      newpost = // newpost.substitute(preterm,prebe); 
                newpost.substituteEq("" + preterm,prebe); 
      processed.add(preterm); 
    }  // substitute(preterm,prebe) more appropriate

    newpost.typeCheck(types,entities,context,atts);  
    String body = Association.genEventCode(env0,null,newpost,false);
    if (returning.length() > 0)
    { body = body + "\n  return" + returning + ";\n"; }

    String res = header + newdecs + preheader + body + "\n" + "  }";
    return res; 
  }

  public String buildUpdateOpJava6(Entity ent, String opname,
                              Type t, String resT, 
                              Vector entities, Vector types)
  { String preheader = ""; 
    String javaPars = getJava6ParameterDec(); 
    
    String header = "  public "; 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    if (isClassScope() || isStatic())
    { header = header + "static "; } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();

    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = new Vector(); 
    atts.addAll(parameters); 

    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { header = header + "  " + resT + " result = " + resultType.getDefaultJava6() + ";\n"; 
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.computeNegation4succ(); 
        preheader = "  //  if (" + npre.queryFormJava6(env0,true) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 

    Expression newpost = (Expression) post.clone(); 
    Vector preterms = post.allPreTerms(); 
    // System.out.println("Pre terms: " + preterms); 
    // substitute these by new variables pre_var in post, copy types/multiplicities
    // of preterm variables. Have initial assignments pre_var = var.queryForm();
    // for each. Can optimise by ignoring vars not on any lhs

    Vector processed = new Vector(); 

    for (int i = 0; i < preterms.size(); i++)
    { BasicExpression preterm = (BasicExpression) preterms.get(i); 
      if (processed.contains(preterm)) { continue; } 

      Type typ = preterm.getType();  // but actual type may be list if multiple
      Type actualtyp; 
      String newdec = ""; 
      String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
      String pretermqf = preterm.queryFormJava6(env0,true); 
      BasicExpression prebe = new BasicExpression(pre_var); 
      
      if (preterm.isMultiple())
      { if (preterm.isOrdered())
        { actualtyp = new Type("Sequence", null); } 
        else 
        { actualtyp = new Type("Set",null); } 
        actualtyp.setElementType(preterm.getElementType()); 

        if (preterm.umlkind == Expression.CLASSID && preterm.arrayIndex == null) 
        { pretermqf = "Controller.inst()." + pretermqf.toLowerCase() + "s"; } 
        String j6type = actualtyp.getJava6(); 
        newdec = j6type + " " + pre_var + " = new " + j6type + "();\n" + 
                 "    " + pre_var + ".addAll(" + pretermqf + ");\n"; 
      } 
      else 
      { actualtyp = typ;
        newdec = actualtyp.getJava6() + " " + pre_var + " = " + pretermqf + ";\n";
      } 
      newdecs = newdecs + "    " + newdec; 
      prebe.type = actualtyp; 
      prebe.elementType = preterm.elementType; 
      Attribute preatt = new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
      // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
      //                    " elemtype= " + prebe.elementType); 
      preatt.setElementType(preterm.elementType); 
      atts.add(preatt); 
      newpost = newpost.substituteEq("" + preterm,prebe); 
      processed.add(preterm); 
    }  // substitute(preterm,prebe) more appropriate

    newpost.typeCheck(types,entities,context,atts);  
    String body = Association.genEventCodeJava6(env0,null,newpost,false);
    if (returning.length() > 0)
    { body = body + "\n  return" + returning + ";\n"; }

    String res = header + newdecs + preheader + body + "\n" + "  }";
    return res; 
  }

  public String buildUpdateOpJava7(Entity ent, String opname,
                              Type t, String resT, 
                              Vector entities, Vector types)
  { String preheader = ""; 
    String javaPars = getJava7ParameterDec(); 
    
    String header = "  public "; 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    if (isClassScope() || isStatic())
    { header = header + "static "; } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();

    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = new Vector(); 
    atts.addAll(parameters); 

    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { header = header + "  " + resT + " result = " + resultType.getDefaultJava7() + ";\n"; 
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.computeNegation4succ(); 
        preheader = "  //  if (" + npre.queryFormJava7(env0,true) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 

    Expression newpost = (Expression) post.clone(); 
    Vector preterms = post.allPreTerms(); 
    // System.out.println("Pre terms: " + preterms); 
    // substitute these by new variables pre_var in post, copy types/multiplicities
    // of preterm variables. Have initial assignments pre_var = var.queryForm();
    // for each. Can optimise by ignoring vars not on any lhs

    Vector processed = new Vector(); 

    for (int i = 0; i < preterms.size(); i++)
    { BasicExpression preterm = (BasicExpression) preterms.get(i); 
      if (processed.contains(preterm)) { continue; } 

      Type typ = preterm.getType();  // but actual type may be list if multiple
      Type actualtyp; 
      String newdec = ""; 
      String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
      String pretermqf = preterm.queryFormJava7(env0,true); 
      BasicExpression prebe = new BasicExpression(pre_var); 
      
      if (preterm.isMultiple())
      { if (preterm.isOrdered())
        { actualtyp = new Type("Sequence", null); } 
        else 
        { actualtyp = new Type("Set",null); } 
        actualtyp.setElementType(preterm.getElementType()); 

        if (preterm.umlkind == Expression.CLASSID && preterm.arrayIndex == null) 
        { pretermqf = "Controller.inst()." + pretermqf.toLowerCase() + "s"; } 
        String j6type = actualtyp.getJava7(preterm.elementType); 
        newdec = j6type + " " + pre_var + " = new " + j6type + "();\n" + 
                 "    " + pre_var + ".addAll(" + pretermqf + ");\n"; 
      } 
      else 
      { actualtyp = typ;
        newdec = actualtyp.getJava7(preterm.elementType) + " " + pre_var + " = " + pretermqf + ";\n";
      } 
      newdecs = newdecs + "    " + newdec; 
      prebe.type = actualtyp; 
      prebe.elementType = preterm.elementType; 
      Attribute preatt = new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
      // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
      //                    " elemtype= " + prebe.elementType); 
      preatt.setElementType(preterm.elementType); 
      atts.add(preatt); 
      newpost = newpost.substituteEq("" + preterm,prebe); 
      processed.add(preterm); 
    }  // substitute(preterm,prebe) more appropriate

    newpost.typeCheck(types,entities,context,atts);  
    String body = Association.genEventCodeJava7(env0,null,newpost,false);
    if (returning.length() > 0)
    { body = body + "\n  return" + returning + ";\n"; }

    String res = header + newdecs + preheader + body + "\n" + "  }";
    return res; 
  }


  public String buildUpdateOpCSharp(Entity ent, String opname,
                              Type t, String resT, 
                              Vector entities, Vector types)
  { String preheader = ""; 
    String javaPars = getCSharpParameterDec(); 
    
    String header = "  public "; 
    if (isAbstract())
    { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "sealed "; } 
    if (isClassScope() || isStatic())
    { header = header + "static "; } 

    if (isAbstract())
    { header = header + resT + " " +
                    opname + "(" + javaPars + ");\n\n";
      return header; 
    }

    header = header + resT + " " +
                    opname + "(" + javaPars + ")\n  { ";

    java.util.Map env0 = new java.util.HashMap();

    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = new Vector(); 
    atts.addAll(parameters); 

    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { header = header + "  " + resT + " result = " + resultType.getDefaultCSharp() + ";\n"; 
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.computeNegation4succ(); 
        preheader = "  //  if (" + npre.queryFormCSharp(env0,true) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 

    Expression newpost = (Expression) post.clone(); 
    Vector preterms = post.allPreTerms(); 
    // System.out.println("Pre terms: " + preterms); 
    // substitute these by new variables pre_var in post, copy types/multiplicities
    // of preterm variables. Have initial assignments pre_var = var.queryForm();
    // for each. Can optimise by ignoring vars not on any lhs

    Vector processed = new Vector(); 

    for (int i = 0; i < preterms.size(); i++)
    { BasicExpression preterm = (BasicExpression) preterms.get(i); 
      if (processed.contains(preterm)) { continue; } 

      Type typ = preterm.getType();  // but actual type may be list if multiple
      Type actualtyp; 
      String newdec = ""; 
      String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
      String pretermqf = preterm.queryFormCSharp(env0,true); 
      BasicExpression prebe = new BasicExpression(pre_var); 
      
      if (preterm.isMultiple())
      { if (preterm.isOrdered())
        { actualtyp = new Type("Sequence",null); } 
        else
        { actualtyp = new Type("Set",null); }
        actualtyp.setElementType(preterm.getElementType()); 
 
        if (preterm.umlkind == Expression.CLASSID && preterm.arrayIndex == null) 
        { pretermqf = "Controller.inst().get" + pretermqf.toLowerCase() + "_s()"; } 
        newdec = actualtyp.getCSharp() + " " + pre_var + " = new ArrayList();\n" + 
                 "    " + pre_var + ".AddRange(" + pretermqf + ");\n"; 
      } 
      else 
      { actualtyp = typ;
        newdec = actualtyp.getCSharp() + " " + pre_var + " = " + pretermqf + ";\n";
      } 
      newdecs = newdecs + "    " + newdec; 
      prebe.type = actualtyp; 
      prebe.elementType = preterm.elementType; 
      Attribute preatt = new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
      // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
      //                    " elemtype= " + prebe.elementType); 
      preatt.setElementType(preterm.elementType); 
      atts.add(preatt); 
      newpost = newpost.substituteEq("" + preterm,prebe); 
      processed.add(preterm); 
    }  // substitute(preterm,prebe) more appropriate

    newpost.typeCheck(types,entities,context,atts);  
    String body = Association.genEventCodeCSharp(env0,null,newpost,false);
    if (returning.length() > 0)
    { body = body + "\n  return" + returning + ";\n"; }

    String res = header + newdecs + preheader + body + "\n" + "  }";
    return res; 
  }

  public String buildUpdateOpCPP(Entity ent, String opname,
                              Type t, String resT, 
                              Vector entities, Vector types, Vector decs)
  { String preheader = ""; 
    String javaPars = getCPPParameterDec(); 
    String ename = ent.getName(); 
    
    String header = "  "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    // if (isFinal())
    // { header = header + "sealed "; } 

    String isstatic = ""; 
    if (isClassScope() || isStatic())
    { isstatic = "static "; } 

    // if (isAbstract())
    // { header = header + resT + " " +
    //                 opname + "(" + javaPars + ");\n\n";
    //   return header; 
    // }

    header = "  " + resT + " " + ename + "::" + 
                    opname + "(" + javaPars + ")\n  { ";
    decs.add("  " + isstatic + resT + " " + opname + "(" + javaPars + ");\n"); 
 
    java.util.Map env0 = new java.util.HashMap();

    if (ent == null || isClassScope() || isStatic()) { } 
    else 
    { env0.put(ent.getName(),"this"); } 

    Vector atts = new Vector(); 
    atts.addAll(parameters); 

    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String et = "void*"; 
    if (elementType != null) 
    { et = elementType.getCPP(elementType.getElementType()); } 

    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { header = header + "  " + resT + " result = " + resultType.getDefaultCPP(elementType) + ";\n"; 
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { pre.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre)) { } 
      else 
      { Expression npre = pre.computeNegation4succ(); 
        preheader = "  //  if (" + npre.queryFormCPP(env0,true) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 

    Expression newpost = (Expression) post.clone(); 
    Vector preterms = post.allPreTerms(); 
    // System.out.println("Pre terms: " + preterms); 
    // substitute these by new variables pre_var in post, copy types/multiplicities
    // of preterm variables. Have initial assignments pre_var = var.queryForm();
    // for each. Can optimise by ignoring vars not on any lhs

    Vector processed = new Vector(); 

    for (int i = 0; i < preterms.size(); i++)
    { BasicExpression preterm = (BasicExpression) preterms.get(i); 
      if (processed.contains(preterm)) { continue; } 

      Type typ = preterm.getType();  // but actual type may be list if multiple
      Type actualtyp; 
      String newdec = ""; 
      String pre_var = Identifier.nextIdentifier("pre_" + preterm.data);
      String pretermqf = preterm.queryFormCPP(env0,true); 
      BasicExpression prebe = new BasicExpression(pre_var); 
      
      Type preET = preterm.getElementType(); 
      String pcet = "void*"; 
      if (preET != null) 
      { pcet = preET.getCPP(preET.getElementType()); } 

      if (preterm.isMultiple())
      { if (preterm.umlkind == Expression.CLASSID && preterm.arrayIndex == null) 
        { pretermqf = "Controller::inst->get" + pretermqf.toLowerCase() + "_s()"; } 
        if (preterm.isOrdered())
        { actualtyp = new Type("Sequence",null); 
          newdec =  "  vector<" + pcet + ">* " + pre_var + " = new vector<" + pcet + ">();\n" + 
                 "    " + pre_var + "->insert(" + pre_var + "->end(), " + 
                                              pretermqf + "->begin(), " + 
                                              pretermqf + "->end());\n";
        } 
        else 
        { actualtyp = new Type("Set",null); 
          newdec =  "  set<" + pcet + ">* " + pre_var + " = new set<" + pcet + ">();\n" + 
                 "    " + pre_var + "->insert(" + pretermqf + "->begin(), " + 
                                              pretermqf + "->end());\n";
        }  
        actualtyp.setElementType(preET); 
      } 
      else 
      { actualtyp = typ;
        newdec = actualtyp.getCPP() + " " + pre_var + " = " + pretermqf + ";\n";
      } 
      newdecs = newdecs + "    " + newdec; 
      prebe.type = actualtyp; 
      prebe.elementType = preterm.elementType; 
      Attribute preatt = new Attribute(pre_var,actualtyp,ModelElement.INTERNAL); 
      // System.out.println("Pre variable " + prebe + " type= " + actualtyp + 
      //                    " elemtype= " + prebe.elementType); 
      preatt.setElementType(preterm.elementType); 
      atts.add(preatt); 
      newpost = newpost.substituteEq("" + preterm,prebe); 
      processed.add(preterm); 
    }  // substitute(preterm,prebe) more appropriate

    newpost.typeCheck(types,entities,context,atts);  
    String body = Association.genEventCodeCPP(env0,null,newpost,false);
    if (returning.length() > 0)
    { body = body + "\n  return" + returning + ";\n"; }

    String res = header + newdecs + preheader + body + "\n" + "  }";
    return res; 
  }


  public String buildGlobalUpdateOp(Entity ent, String opname,
                                    Type t, String resT, 
                                    Vector entities, Vector types,
                                    Vector constraints)
  { String preheader = ""; 
    if (ent == null) { return ""; } 

    String javaPars = getJavaParameterDec(); 
    String parlist = parameterList(); 
    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 

    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (!instanceScope)
    { if (resT == null || resT.equals("void")) 
      { return "  public static void " + opname + "(" + javaPars + ")\n" + 
               "  { " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
      } 
      return " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
             " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 
    
    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "List " + ex + "s"; 
    } 
    else 
    { java2Pars = "List " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 

    String retT = resT; 
    if (resT == null)
    { retT = "void"; } 

    String header2 = header + " List All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    
    header = header + retT + " " +
                    opname + "(" + javaPars + ")\n  { ";
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) 
    { header2 = header2 + "\n    List result = new Vector();\n  "; 
      retT = "void"; 
    }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefault() + ";\n  "; 
      header2 = header2 + "\n    List result = new Vector();\n  ";
      returning = " result"; 
    }

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "  //  if (!(" + pre1.queryForm(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    String body = "  " + ex + "." + call + ";"; 
    String endbody = ""; 
    header2 = header2 + "  for (int _i = 0; _i < " + exs + ".size(); _i++)\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") " + exs + ".get(_i);\n"; 

    if (parlist.length() > 0)
    { parlist = "," + parlist; } 

    String newitem = opname + "(" + ex + parlist + ")"; 

    if ("List".equals(resT))
    { header2 = header2 + "      result.addAll(" + newitem + ");\n"; }
    else if (resT == null || resT.equals("void")) 
    { header2 = header2 + "      " + newitem + ";\n"; }
    else if (resultType.isPrimitive())
    { newitem = Expression.wrap(resultType,newitem); 
      header2 = header2 + "      result.add(" + newitem + ");\n"; 
    }
    else 
    { header2 = header2 + "      result.add(" + newitem + ");\n"; }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    if (resT == null || resT.equals("void")) 
    { }
    else
    { body = " result = " + body; 
      endbody = "    return result;"; 
    }

    for (int i = 0; i < constraints.size(); i++)
    { Constraint cc = (Constraint) constraints.get(i); 
      Constraint ccnew = cc.matchesOp(opname, this);
      if (ccnew != null) 
      { Vector contx = new Vector(); 
        if (ccnew.getOwner() != null) 
        { contx.add(ccnew.getOwner()); } 
        boolean ok = ccnew.typeCheck(types,entities,contx); 
        if (ok)
        { String upd = ccnew.globalUpdateOp(ent,false); 
          body = body + "\n    " + upd;
        } 
      } 
    }  

    // if (isDefinedinSuperclass())
    // { return header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; } 
    String gop = header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; 
    if (derived) 
    { return gop; } 
    return gop + header2;
  }

  public String buildGlobalUpdateOpJava6(Entity ent, String opname,
                                    Type t, String resT, 
                                    Vector entities, Vector types,
                                    Vector constraints)
  { String preheader = ""; 
    String javaPars = getJava6ParameterDec(); 
    String parlist = parameterList(); 

    if (ent == null) { return ""; } 
    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    String retT = resT; 
    if (resT == null)
    { retT = "void"; } 

    if (!instanceScope)
    { if (resT == null || resT.equals("void")) 
      { return "  public static void " + opname + "(" + javaPars + ")\n" + 
               "  { " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
      } 
      return " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
             " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 
    
    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "Collection " + ex + "s"; 
    } 
    else 
    { java2Pars = "Collection " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 

    String header2 = header + " ArrayList All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    
    header = header + retT + " " +
                    opname + "(" + javaPars + ")\n  { ";
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) 
    { header2 = header2 + "\n    ArrayList result = new ArrayList();\n  "; }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefaultJava6() + ";\n  "; 
      header2 = header2 + "\n    ArrayList result = new ArrayList();\n  ";
      returning = " result"; 
    }

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "  //  if (!(" + pre1.queryFormJava6(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    String body = "  " + ex + "." + call + ";"; 
    String endbody = ""; 
    header2 = header2 + "  for (Object _i : " + exs + ")\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") _i;\n"; 

    if (parlist.length() > 0)
    { parlist = "," + parlist; } 

    String newitem = opname + "(" + ex + parlist + ")"; 

    if (resultType != null && Type.isCollectionType(resultType))
    { header2 = header2 + "      result.addAll(" + newitem + ");\n"; }
    else if (resT == null || resT.equals("void")) 
    { header2 = header2 + "      " + newitem + ";\n"; }
    else if (resultType.isPrimitive())
    { newitem = Expression.wrap(resultType,newitem); 
      header2 = header2 + "      result.add(" + newitem + ");\n"; 
    }
    else 
    { header2 = header2 + "      result.add(" + newitem + ");\n"; }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    if (resT == null || resT.equals("void")) 
    { }
    else
    { body = " result = " + body; 
      endbody = "    return result;"; 
    }

    for (int i = 0; i < constraints.size(); i++)
    { Constraint cc = (Constraint) constraints.get(i); 
      Constraint ccnew = cc.matchesOp(opname, this);
      if (ccnew != null) 
      { Vector contx = new Vector(); 
        if (ccnew.getOwner() != null) 
        { contx.add(ccnew.getOwner()); } 
        boolean ok = ccnew.typeCheck(types,entities,contx); 
        if (ok)
        { String upd = ccnew.globalUpdateOpJava6(ent,false); 
          body = body + "\n    " + upd;
        } 
      } 
    }  

    // if (isDefinedinSuperclass())
    // { return header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; } 
    String gop = header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; 
    if (derived) 
    { return gop; } 
    return gop + header2;
  }

  public String buildGlobalUpdateOpJava7(Entity ent, String opname,
                                    Type t, String resT, 
                                    Vector entities, Vector types,
                                    Vector constraints)
  { String preheader = ""; 
    String javaPars = getJava7ParameterDec(); 
    String parlist = parameterList(); 

    if (ent == null) { return ""; } 
    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (!instanceScope)
    { if (resT == null || resT.equals("void")) 
      { return "  public static void " + opname + "(" + javaPars + ")\n" + 
               "  { " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
      } 
      return " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
             " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 
    
    Type allrestype = new Type("Sequence", null); 
    allrestype.setElementType(resultType); 
    String alltype = allrestype.getJava7(resultType); 

    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "Collection<" + ename + "> " + ex + "s"; 
    } 
    else 
    { java2Pars = "Collection<" + ename + "> " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 

    String retT = resT; 
    String header2 = header; 
    if (resT == null || resT.equals("void")) 
    { header2 = header2 + " ArrayList All" + ename + 
                     opname + "(" + java2Pars + ")\n  { "; 
      retT = "void"; 
    } 
    else 
    { header2 = header2 + " " + alltype + " All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    } 

    header = header + retT + " " +
                    opname + "(" + javaPars + ")\n  { ";
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) 
    { header2 = header2 + "\n    ArrayList result = new ArrayList();\n  "; }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefaultJava7() + ";\n  "; 
      header2 = header2 + "\n    " + alltype + " result = new " + alltype + "();\n  ";
      returning = " result"; 
    } // wrapper types needed for primitive. 

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "  //  if (!(" + pre1.queryFormJava7(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    String body = "  " + ex + "." + call + ";"; 
    String endbody = ""; 
    header2 = header2 + "  for (Object _i : " + exs + ")\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") _i;\n"; 

    if (parlist.length() > 0)
    { parlist = "," + parlist; } 

    String newitem = opname + "(" + ex + parlist + ")"; 

    if (resultType != null && Type.isCollectionType(resultType))
    { header2 = header2 + "      result.addAll(" + newitem + ");\n"; }
    else if (resT == null || resT.equals("void")) 
    { header2 = header2 + "      " + newitem + ";\n"; }
    else if (resultType.isPrimitive())
    { newitem = Expression.wrap(resultType,newitem); 
      header2 = header2 + "      result.add(" + newitem + ");\n"; 
    }
    else 
    { header2 = header2 + "      result.add(" + newitem + ");\n"; }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    if (resT == null || resT.equals("void")) 
    { }
    else
    { body = " result = " + body; 
      endbody = "    return result;"; 
    }

    for (int i = 0; i < constraints.size(); i++)
    { Constraint cc = (Constraint) constraints.get(i); 
      Constraint ccnew = cc.matchesOp(opname, this);
      if (ccnew != null) 
      { Vector contx = new Vector(); 
        if (ccnew.getOwner() != null) 
        { contx.add(ccnew.getOwner()); } 
        boolean ok = ccnew.typeCheck(types,entities,contx); 
        if (ok)
        { String upd = ccnew.globalUpdateOpJava7(ent,false); 
          body = body + "\n    " + upd;
        } 
      } 
    }  

    // if (isDefinedinSuperclass())
    // { return header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; } 
    String gop = header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; 
    if (derived) 
    { return gop; } 
    return gop + header2;
  }

  public String buildGlobalUpdateOpCSharp(Entity ent, String opname,
                                    Type t, String resT, 
                                    Vector entities, Vector types,
                                    Vector constraints)
  { String preheader = ""; 
    String javaPars = getCSharpParameterDec(); 
    String parlist = parameterList(); 

    if (ent == null) { return ""; } 
    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "sealed "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(ArrayList exs, pars)
    String java2Pars = "";  // For All version of op 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    String retT = resT; 
    if (resT == null)
    { retT = "void"; } 

    if (!instanceScope)
    { if (resT == null || resT.equals("void")) 
      { return "  public static void " + opname + "(" + javaPars + ")\n" + 
               "  { " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
      } 
      return " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
             " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 
    
    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "ArrayList " + ex + "s"; 
    } 
    else 
    { java2Pars = "ArrayList " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 

    String header2 = header + " ArrayList All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    
    header = header + retT + " " +
                    opname + "(" + javaPars + ")\n  { ";
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resT == null || resT.equals("void")) 
    { header2 = header2 + "\n    ArrayList result = new ArrayList();\n  "; }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefaultCSharp() + ";\n  "; 
      header2 = header2 + "\n    ArrayList result = new ArrayList();\n  ";
      returning = " result"; 
    }

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "  //  if (!(" + pre1.queryFormCSharp(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    String body = "  " + ex + "." + call + ";"; 
    String endbody = ""; 
    header2 = header2 + "  for (int _i = 0; _i < " + exs + ".Count; _i++)\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") " + exs + "[_i];\n"; 

    if (parlist.length() > 0)
    { parlist = "," + parlist; } 

    String newitem = opname + "(" + ex + parlist + ")"; 

    if (resultType != null && Type.isCollectionType(resultType))
    { header2 = header2 + "      result.AddRange(" + newitem + ");\n"; }
    else if (resT == null || resT.equals("void")) 
    { header2 = header2 + "      " + newitem + ";\n"; }
    else if (resultType.isPrimitive())
    { // newitem = Expression.wrap(resultType,newitem); 
      header2 = header2 + "      result.Add(" + newitem + ");\n"; 
    }
    else 
    { header2 = header2 + "      result.Add(" + newitem + ");\n"; }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    if (resT == null || resT.equals("void")) 
    { }
    else
    { body = " result = " + body; 
      endbody = "    return result;"; 
    }

    for (int i = 0; i < constraints.size(); i++)
    { Constraint cc = (Constraint) constraints.get(i); 
      Constraint ccnew = cc.matchesOp(opname, this);
      if (ccnew != null) 
      { Vector contx = new Vector(); 
        if (ccnew.getOwner() != null) 
        { contx.add(ccnew.getOwner()); } 
        boolean ok = ccnew.typeCheck(types,entities,contx); 
        if (ok)
        { String upd = ccnew.globalUpdateOpCSharp(ent,false); 
          body = body + "\n    " + upd;
        } 
      } 
    }  

    // if (isDefinedinSuperclass())
    // { return header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; } 

    String gop = header + newdecs + preheader + body + "\n " + endbody + "  }\n\n";  
    if (derived) 
    { return gop; } 
    return gop + header2;
  }  // if derived, omit the All global one. 

  public String buildGlobalUpdateOpCPP(Entity ent, String opname,
                                    Type t, String resT, Vector declarations,  
                                    Vector entities, Vector types,
                                    Vector constraints)
  { String preheader = ""; 
    String javaPars = getCPPParameterDec(); 

    if (ent == null) { return ""; } 
    
    String header = "  "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    // if (isFinal())
    // { header = header + "sealed "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(ArrayList exs, pars)
    String java2Pars = "";  // For All version of op 
    String java3Pars = ""; 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 
    
    if (javaPars.equals(""))
    { javaPars = ename + "* " + ex;
      java2Pars = "vector<" + ename + "*>* " + exs; 
      java3Pars = "set<" + ename + "*>* " + exs; 
    } 
    else 
    { java2Pars = "vector<" + ename + "*>* " + exs + ", " + javaPars; 
      javaPars = ename + "* " + ex + ", " + javaPars; 
      java3Pars = "set<" + ename + "*>* " + exs + ", " + javaPars; 
    } 

    String et = "void*"; 
    if (elementType != null) 
    { et = elementType.getCPP(elementType.getElementType()); } 

    String allrest = "vector<void*>*";
    if (resultType != null)
    { allrest = "vector<" + resT + ">*"; } 

    String header2 = header + " " + allrest + " Controller::All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    String header3 = header + " " + allrest + " Controller::All" + ename + 
                     opname + "(" + java3Pars + ")\n  { ";

    if (derived) { } 
    else 
    { declarations.add("  " + allrest + " All" + ename + opname + "(" + java2Pars + ");"); 
      declarations.add("  " + allrest + " All" + ename + opname + "(" + java3Pars + ");");
    } 

    String retT = resT; 
    if (resT == null)
    { retT = "void"; } 
    
    header = header + retT + " Controller::" +
                             opname + "(" + javaPars + ")\n  { ";
    declarations.add(retT + " " + opname + "(" + javaPars + ");"); 

    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);

    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    resultatt.setElementType(elementType); 
    atts.add(resultatt);

    String returning = "";
    if (resultType == null || resT.equals("void")) 
    { header2 = header2 + "\n    vector<void*>* result = new vector<void*>();\n  "; 
      header3 = header3 + "\n    vector<void*>* result = new vector<void*>();\n  ";
    }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefaultCPP(elementType) + ";\n  "; 
      header2 = header2 + "\n    vector<" + resT + ">* result = new vector<" + resT + ">();\n  ";
      header3 = header3 + "\n    vector<" + resT + ">* result = new vector<" + resT + ">();\n  ";
      returning = " result"; 
    }

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "  //  if (!(" + pre1.queryFormCPP(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    String body = "  " + ex + "->" + call + ";"; 
    String endbody = ""; 
    header2 = header2 + "  for (int _i = 0; _i < " + exs + "->size(); _i++)\n" +
              "    { " + ename + "* " + ex + " = (*" + exs + ")[_i];\n"; 
    header3 = header3 + "  for (set<" + ename + "*>::iterator _i = " + exs + "->begin(); _i != " + exs + "->end(); _i++)\n" +
              "    { " + ename + "* " + ex + " = *_i;\n"; 

    String parlist = parameterList(); 
    if (parlist.length() > 0)
    { parlist = "," + parlist; } 

    String newitem = opname + "(" + ex + parlist + ")"; 

    if (resultType != null && Type.isCollectionType(resultType))
    { header2 = header2 + "      result->insert(result->end(), " + newitem + "->begin(), " + 
                                                newitem + "->end());\n"; 
      header3 = header3 + "      result->insert(result->end(), " + newitem + "->begin(), " + 
                                                newitem + "->end());\n"; 
    }
    else if (resultType == null || resT.equals("void")) 
    { header2 = header2 + "      " + newitem + ";\n"; 
      header3 = header3 + "      " + newitem + ";\n";
    }
    else 
    { header2 = header2 + "      result->push_back(" + newitem + ");\n"; 
      header3 = header3 + "      result->push_back(" + newitem + ");\n"; 
    }

    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 
    header3 = header3 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    if (resultType == null || resT.equals("void")) 
    { }
    else
    { body = " result = " + body; 
      endbody = "    return result;"; 
    }

    for (int i = 0; i < constraints.size(); i++)
    { Constraint cc = (Constraint) constraints.get(i); 
      Constraint ccnew = cc.matchesOp(opname, this);
      if (ccnew != null) 
      { Vector contx = new Vector(); 
        if (ccnew.getOwner() != null) 
        { contx.add(ccnew.getOwner()); } 
        boolean ok = ccnew.typeCheck(types,entities,contx); 
        if (ok)
        { String upd = ccnew.globalUpdateOpCPP(ent,false); 
          body = body + "\n    " + upd;
        } 
      } 
    }  

    // if (isDefinedinSuperclass())
    // { return header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; } 
    if (derived) 
    { return header + newdecs + preheader + body + "\n " + endbody + "  }\n\n"; } 
    else 
    { return header + newdecs + preheader + body + "\n " + endbody + "  }\n\n" + header2 + header3; } 
  }


  public String buildGlobalQueryOp(Entity ent, String opname,
                                   Type t, String resT, 
                                   Vector entities, Vector types)
  { // if (isDefinedinSuperclass())
    // { return ""; } 
    if (ent == null) { return ""; } 

    String preheader = ""; 
    String javaPars = getJavaParameterDec(); 
    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 
    String parlist = parameterList(); 
    
    if (!instanceScope) 
    { return ""; // " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
                 // " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 

    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "List " + exs; 
    } 
    else 
    { java2Pars = "List " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 

    String header2 = header + " List All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);
    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    atts.add(resultatt);
    String returning = "";
    String retT = resT; 
    if (resT == null || resT.equals("void")) 
    { retT = "void"; }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefault() + ";\n  "; 
      header2 = header2 + "\n    List result = new Vector();\n  ";
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "    if (!(" + pre1.queryForm(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    }   // 5300 

    String newdecs = ""; 
    String call = toString();
     
    header2 = header2 + "  for (int _i = 0; _i < " + exs + ".size(); _i++)\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") " + exs + ".get(_i);\n"; 

    // if (parlist.length() > 0)
    // { parlist = "," + parlist; } 

    String newitem = ex + "." + opname + "(" + parlist + ")"; 

    if ("List".equals(resT))
    { header2 = header2 + "      result.addAll(" + newitem + ");\n"; }
    else 
    { if (t != null && t.isPrimitive())
      { newitem = Expression.wrap(resultType,newitem); }
      header2 = header2 + "      result.add(" + newitem + ");\n"; 
    }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    return header2;
  }

  public String buildGlobalQueryOpJava6(Entity ent, String opname,
                                   Type t, String resT, 
                                   Vector entities, Vector types)
  { if (derived)
    { return ""; }
    if (ent == null) { return ""; } 
 
    String preheader = ""; 
    String javaPars = getJava6ParameterDec(); 
    String parlist = parameterList(); 

    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 

    if (!instanceScope) 
    { return ""; // " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
             // " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 
    
    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "Collection " + exs; 
    } 
    else 
    { java2Pars = "Collection " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 



    String header2 = header + " ArrayList All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);
    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    atts.add(resultatt);
    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefaultJava6() + ";\n  "; 
      header2 = header2 + "\n    ArrayList result = new ArrayList();\n  ";
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "    if (!(" + pre1.queryFormJava6(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    header2 = header2 + "  for (Object _i : " + exs + ")\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") _i;\n"; 

    // if (parlist.length() > 0)
    // { parlist = "," + parlist; } 

    String newitem = ex + "." + opname + "(" + parlist + ")"; 

    if (resultType != null && Type.isCollectionType(resultType))
    { header2 = header2 + "      result.addAll(" + newitem + ");\n"; }
    else 
    { if (t != null && t.isPrimitive())
      { newitem = Expression.wrap(resultType,newitem); }
      header2 = header2 + "      result.add(" + newitem + ");\n"; 
    }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    return header2;
  }

  public String buildGlobalQueryOpJava7(Entity ent, String opname,
                                   Type t, String resT, 
                                   Vector entities, Vector types)
  { if (derived)
    { return ""; }
    if (ent == null) { return ""; } 
 
    String preheader = ""; 
    String javaPars = getJava7ParameterDec(); 
    String parlist = parameterList(); 

    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "final "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 

    if (!instanceScope) 
    { return ""; // " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
             // " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 
    
    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "Collection<" + ename + "> " + exs; 
    } 
    else 
    { java2Pars = "Collection<" + ename + "> " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 

    Type allrestype = new Type("Sequence", null); 
    allrestype.setElementType(resultType); 
    String alltype = allrestype.getJava7(resultType); 

    String header2 = header; 
    if (resT == null || resT.equals("void")) 
    { header2 = header2 + " ArrayList All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    } // not valid case
    else 
    { header2 = header2 + " " + alltype + " All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    } 

    
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);
    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    atts.add(resultatt);
    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { header = header + "\n   " + resT + " result = " + resultType.getDefaultJava7() + ";\n  "; 
      header2 = header2 + "\n    " + alltype + " result = new " + alltype + "();\n  ";
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "    if (!(" + pre1.queryFormJava7(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    header2 = header2 + "  for (Object _i : " + exs + ")\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") _i;\n"; 

    // if (parlist.length() > 0)
    // { parlist = "," + parlist; } 

    String newitem = ex + "." + opname + "(" + parlist + ")"; 

    if (resultType != null && Type.isCollectionType(resultType))
    { header2 = header2 + "      result.addAll(" + newitem + ");\n"; }
    else 
    { if (t != null && t.isPrimitive())
      { newitem = Expression.wrap(resultType,newitem); }
      header2 = header2 + "      result.add(" + newitem + ");\n"; 
    }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    return header2;
  }

  public String buildGlobalQueryOpCSharp(Entity ent, String opname,
                                   Type t, String resT, 
                                   Vector entities, Vector types)
  { // if (isDefinedinSuperclass())
    // { return ""; } 
    if (ent == null || derived) { return ""; } 

    String preheader = ""; 
    String javaPars = getCSharpParameterDec(); 
    
    String header = "  public "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    if (isFinal())
    { header = header + "sealed "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 
    String parlist = parameterList(); 

    if (!instanceScope) 
    { return ""; // " public static " + resT + " " + opname + "(" + javaPars + ")\n" + 
             // " { return " + ename + "." + opname + "(" + parlist + "); }\n\n"; 
    } 
    
    if (javaPars.equals(""))
    { javaPars = ename + " " + ex;
      java2Pars = "ArrayList " + exs; 
    } 
    else 
    { java2Pars = "ArrayList " + exs + "," + javaPars; 
      javaPars = ename + " " + ex + "," + javaPars; 
    } 

    String header2 = header + " ArrayList All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);
    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    atts.add(resultatt);
    String returning = "";
    if (resT == null || resT.equals("void")) { }
    else
    { // header = header + "\n   " + resT + " result = " + resultType.getDefaultCSharp() + ";\n  "; 
      header2 = header2 + "\n    ArrayList result = new ArrayList();\n  ";
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "    if (!(" + pre1.queryFormCSharp(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    header2 = header2 + "  for (int _i = 0; _i < " + exs + ".Count; _i++)\n" +
              "    { " + ename + " " + ex + " = (" + ename + ") " + exs + "[_i];\n"; 

    // if (parlist.length() > 0)
    // { parlist = "," + parlist; } 

    String newitem = ex + "." + opname + "(" + parlist + ")"; 

    if ("ArrayList".equals(resT))
    { header2 = header2 + "      result.AddRange(" + newitem + ");\n"; }
    else 
    { // if (t.isPrimitive())
      // { newitem = Expression.wrap(resultType,newitem); }
      header2 = header2 + "      result.Add(" + newitem + ");\n"; 
    }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    return header2;
  }

  public String buildGlobalQueryOpCPP(Entity ent, String opname,
                                   Type t, String resT, Vector declarations, 
                                   Vector entities, Vector types)
  { if (derived) { return ""; } 
    if (ent == null) { return ""; } 

    // if (isDefinedinSuperclass())
    // { return ""; } 
    String preheader = ""; 
    String javaPars = getCPPParameterDec(); 
    
    String header = "  "; 
    // if (isAbstract())
    // { header = header + "abstract "; } 
    // if (isFinal())
    // { header = header + "sealed "; } 
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    String exs = ex + "s";  // For All version of op: op(List exs, pars)
    String java2Pars = "";  // For All version of op 
    String java3Pars = "";  // For All version of op 
    
    if (javaPars.equals(""))
    { javaPars = ename + "* " + ex;
      java2Pars = "vector<" + ename + "*>* " + exs; 
      java3Pars = "set<" + ename + "*>* " + exs; 
    } 
    else 
    { java2Pars = "vector<" + ename + "*>* " + exs + "," + javaPars; 
      java3Pars = "set<" + ename + "*>* " + exs + "," + javaPars; 
      javaPars = ename + "* " + ex + "," + javaPars; 
    } 

    String restype = "";
    if (resultType == null || resT.equals("void")) 
    { restype = "vector<void*>"; }
    else
    { restype = "vector<" + resT + ">"; }

    String et = "void*"; 
    if (elementType != null) 
    { et = elementType.getCPP(elementType.getElementType()); } 

    String header2 = header + " " + restype + "* Controller::All" + ename + 
                     opname + "(" + java2Pars + ")\n  { ";
    String header3 = header + " " + restype + "* Controller::All" + ename + 
                     opname + "(" + java3Pars + ")\n  { ";
    // and add header to the declarations
    declarations.add(restype + "* All" + ename + opname + "(" + java2Pars + ");"); 
    declarations.add(restype + "* All" + ename + opname + "(" + java3Pars + ");"); 
    
    java.util.Map env0 = new java.util.HashMap();
    env0.put(ename,ex);
    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    atts.add(resultatt);

    String returning = "";
    if (resultType == null || resT.equals("void")) 
    { header2 = header2 + "\n    vector<void*>* result = new vector<void*>();\n  "; 
      header3 = header3 + "\n    vector<void*>* result = new vector<void*>();\n  "; 
    }
    else
    { header2 = header2 + "\n    vector<" + resT + ">* result = new vector<" + resT + ">();\n  ";
      header3 = header3 + "\n    vector<" + resT + ">* result = new vector<" + resT + ">();\n  ";
      returning = " result"; 
    }

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (pre != null) 
    { Expression pre1 = pre.substituteEq("self",new BasicExpression(ex)); 
      pre1.typeCheck(types,entities,context,atts); 
      if ("true".equals("" + pre1)) { } 
      else 
      { preheader = "    if (!(" + pre1.queryFormCPP(env0,false) + 
                    ")) { return" + returning + "; } \n  "; 
      }
    } 

    String newdecs = ""; 
    String call = toString();
     
    header2 = header2 + "  for (int _i = 0; _i < " + exs + "->size(); _i++)\n" +
              "    { " + ename + "* " + ex + " = (*" + exs + ")[_i];\n"; 
    header3 = header3 + "  for (set<" + ename + "*>::iterator _i = " + exs + "->begin(); _i != " + exs + "->end(); _i++)\n" +
              "    { " + ename + "* " + ex + " = *_i;\n"; 

    String parlist = parameterList(); 
    // if (parlist.length() > 0)
    // { parlist = "," + parlist; } 

    String newitem = ex + "->" + opname + "(" + parlist + ")"; 

    if (resultType != null && Type.isCollectionType(resultType))
    { header2 = header2 + "      result->insert(result->end(), " + newitem + "->begin(), " +
                                                newitem + "->end());\n"; 
      header3 = header3 + "      result->insert(result->end(), " + newitem + "->begin(), " +
                                                newitem + "->end());\n"; 
    }
    else 
    { header2 = header2 + "      result->push_back(" + newitem + ");\n"; 
      header3 = header3 + "      result->push_back(" + newitem + ");\n"; 
    }
    
    header2 = header2 + "    }\n" + "    return result; \n" + "  }\n\n"; 
    header3 = header3 + "    }\n" + "    return result; \n" + "  }\n\n"; 

    return header2 + header3;
  }

  // Use the statemachine?
  public BOp getBOperationCode(Entity ent,Vector entities, Vector types)
  { String name = getName();
    String resT = null; 
    if (post == null) 
    { return null; } 
    if (resultType != null)
    { resT = resultType.generateB(); }
    if (query)
    { BOp op = buildBQueryOp(ent,name,resultType,resT,entities,types);
      if (op != null)
      { op.setSignature(getSignature()); }
      return op;
    }
    else
    { BOp op = buildBUpdateOp(ent,name,resultType,resT,entities,types);
      if (op != null)
      { op.setSignature(getSignature()); }
      return op;
    }
  } // no pre?

  public BOp getGlobalBOperationCode(Entity ent,Vector entities, Vector types,
                                     Vector constraints)
  { String name = getName();
    String resT = null; 
    if (resultType != null)
    { resT = resultType.generateB(); }
    if (post == null || query || isAbstract())
    { return null; }
    else 
    { return buildGlobalBUpdateOp(ent,name,resultType,resT,
                                  entities,types,constraints); 
    }
  } // ignore return type for update ops for now. 

  public BOp buildBQueryOp(Entity ent, String opname,
                                  Type t, String resT, 
                                  Vector entities, Vector types)
  { String rx = "resultx";
    Expression rxbe = new BasicExpression(rx);
    BExpression rxbbe = new BBasicExpression(rx); 
    boolean norange = false; 
    

    Scope scope = post.resultScope();
    BinaryExpression rscope = null; 
    if (scope == null) // no range, must assign result
    { System.err.println("ERROR: No scope for result of " + opname); 
      // ANY resultx WHERE resultx: resultType & Post[resultx/result]
      // THEN result := resultx END
      // return null;
      Expression resTbe = new BasicExpression(resT);
      rscope = new BinaryExpression(":",rxbe,resTbe);  
      norange = true; 
      // post = new BinaryExpression("&",post,rscope); 
    }
    else 
    { rscope = scope.resultScope; } 
    
    BExpression returnbe = new BBasicExpression("result");
    java.util.Map env0 = new java.util.HashMap();
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    BExpression exbe = new BBasicExpression(ex); 
    if (instanceScope) 
    { env0.put(ename,exbe); }
    String es = ename.toLowerCase() + "s";
    BExpression esbe = new BBasicExpression(es); 

    Expression presb = new BasicExpression("true"); 
    if (pre != null) 
    { presb = pre.substituteEq("self",new BasicExpression(ex)); } 

    BExpression pre0 = getBParameterDec(); 
    BExpression pre1 = 
      BBinaryExpression.simplify("&",pre0,presb.binvariantForm(env0,false),true); 
    BExpression inst = new BBinaryExpression(":",exbe,esbe); 
    if (instanceScope)
    { pre1 = BBinaryExpression.simplify("&",inst,pre1,false); }
    // Only for instance scope ops 
    Vector atts = (Vector) parameters.clone(); 
    Attribute resultatt = new Attribute("result",t,ModelElement.INTERNAL); 
    Attribute resultxatt = new Attribute(rx,t,ModelElement.INTERNAL); 
    resultatt.setEntity(ent); // seems like a good idea
    resultxatt.setEntity(ent); 
    atts.add(resultatt); 
    atts.add(resultxatt);   

    Vector pars = getParameterNames();
    if (instanceScope) 
    { pars.add(0,ex); } 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    if (isInScope(rscope) || isSubScope(rscope) || 
        (scope != null && scope.scopeKind.equals("array")))
    { Expression newpost = post.substituteEq("result",rxbe); 
   
      newpost.typeCheck(types,entities,context,atts);
      BExpression pred = newpost.binvariantForm(env0,false);
      if (norange)
      { pred = new BBinaryExpression("&",
                 new BBinaryExpression(":",rxbbe,new BBasicExpression(resT)),
                 pred); 
      } 
      else if (scope != null && scope.scopeKind.equals("array"))
      { BasicExpression arr = (BasicExpression) scope.arrayExp.clone();
        arr.arrayIndex = null; 
        BExpression barr = arr.binvariantForm(env0,false); 
        pred = new BBinaryExpression("&",
                 new BBinaryExpression(":",rxbbe,new BUnaryExpression("dom",barr)),
                 pred); 
      } 
      Vector var = new Vector();
      var.add(rx);
      BStatement assign = new BAssignStatement(returnbe,rxbbe); 
      BStatement body = new BAnyStatement(var,pred,assign);
      return new BOp(opname,"result",pars,pre1,body);  
    }
    if (isEqScope(rscope))
    { /* Expression returnval = returnValue(rscope);
      returnval.typeCheck(types,entities,atts); 
      BStatement body =
        new BAssignStatement(returnbe,returnval.binvariantForm(env0,false));
      body.setWriteFrame("result");

      if (post instanceof BinaryExpression)
      { BinaryExpression postbe = (BinaryExpression) post;
        if (postbe.operator.equals("=>"))
        { Expression cond = postbe.left;
          cond.typeCheck(types,entities,atts);
          BStatement code = new BIfStatement(cond.binvariantForm(env0,false),body,
                                             null);
          return new BOp(opname,"result",pars,pre1,code);
        }
      }  // if a conjunction, use separateUpdates(statements)
      */ 
      BStatement body = buildBUpdateCode(rscope,env0,types,entities,atts); 
      return new BOp(opname,"result",pars,pre1,body); 
    }  
    return null;  // result := (opname_ename(ex))(pars)
  }

  private BStatement buildBQueryCases(Expression pst, java.util.Map env,
                                      Vector types, Vector entities, Vector atts)
  { Vector context = new Vector(); 
    if (entity != null) 
    { context.add(entity); } 

    if (pst instanceof BinaryExpression)
    { BinaryExpression bepst = (BinaryExpression) pst; 
      String op = bepst.operator; 

      if (op.equals("="))
      { BExpression returnbe = new BBasicExpression("result");
        Scope scope = bepst.resultScope(); 
        Expression returnval = returnValue(scope.resultScope);
        returnval.typeCheck(types,entities,context,atts); 
        BStatement body =
          new BAssignStatement(returnbe,returnval.binvariantForm(env,false));
        body.setWriteFrame("result");
        return body; 
      } 
      if (op.equals("=>"))
      { BStatement body = buildBUpdateCode(bepst.right, env, types, entities, atts); 
        Expression cond = bepst.left;
        cond.typeCheck(types,entities,context,atts);
        BStatement code = new BIfStatement(cond.binvariantForm(env,false),body,
                                           null);
        code.setWriteFrame("result"); 
        return code; 
      } 
      if (op.equals("&"))
      { Vector res = new Vector(); 
        BStatement code1 = buildBQueryCases(bepst.left, env,types,entities,atts); 
        BStatement code2 = buildBQueryCases(bepst.right, env,types,entities,atts); 
        if ((code1 instanceof BIfStatement) && (code2 instanceof BIfStatement))
        { BIfStatement if1 = (BIfStatement) code1; 
          BIfStatement if2 = (BIfStatement) code2; 
          if1.extendWithCase(if2); 
          return if1;
        }  
        res.add(code1); 
        res.add(code2);
        return BStatement.separateUpdates(res);
      } 
    } // for or, a choice 
    return pst.bupdateForm(env,false); 
  } 
          

  public BExpression buildBConstantDefinition(Entity ent, 
                                              Vector entities, Vector types)
  { // (opname_E: es --> (parT --> resT)) &
    // !ex.(ex : es => !x.(x: parT & pre => post[opname_E(ex,x)/result]))
    
    BExpression fdec = getBFunctionDec(); 
    Expression fcall = new BasicExpression("" + getBCall()); 
    java.util.Map env0 = new java.util.HashMap();
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    BExpression exbe = new BBasicExpression(ex); 
    env0.put(ename,exbe);
    String es = ename.toLowerCase() + "s";
    BExpression esbe = new BBasicExpression(es); 


    BExpression pre0 = getBParameterDec(); 
    BExpression pre1; 
    if (pre == null) 
    { pre1 = pre0; } 
    else 
    { pre1 = 
        BBinaryExpression.simplify("&",pre0,pre.binvariantForm(env0,false),true); 
    } 
    BExpression inst = new BBinaryExpression(":",exbe,esbe); 
    pre1 = BBinaryExpression.simplify("&",inst,pre1,false);
    // Only for instance scope ops 
    Vector atts = (Vector) parameters.clone(); 

    Vector pars = getParameterNames();
    pars.add(0,ex); 

    Expression newpost = post.substituteEq("result",fcall); 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    newpost.typeCheck(types,entities,context,atts);
    BExpression pred = newpost.binvariantForm(env0,false);
    pred.setBrackets(true); 
    
    BExpression fdef = 
      new BQuantifierExpression("forall",pars,
            new BBinaryExpression("=>",pre1,pred)); 
    return new BBinaryExpression("&",fdec,fdef); 
  }


  private BStatement buildBUpdateCode(Expression rscope, java.util.Map env,
                                      Vector types, Vector entities, Vector atts)
  { if (rscope instanceof BinaryExpression)
    { return buildBUpdateCode((BinaryExpression) rscope,env,types,entities,atts); } 
    return null; 
  } 

  private BStatement buildBUpdateCode(BinaryExpression rscope, java.util.Map env,
                                      Vector types, Vector entities, Vector atts)
  { String op = rscope.operator; 
    BExpression returnbe = new BBasicExpression("result");

    Vector context = new Vector(); 
    if (entity != null) 
    { context.add(entity); } 

    if (op.equals("="))
    { Expression returnval = returnValue(rscope);
      returnval.typeCheck(types,entities,context,atts); 
      BStatement body =
        new BAssignStatement(returnbe,returnval.binvariantForm(env,false));
      body.setWriteFrame("result");
      return body; 
    } 
    if (op.equals("=>"))
    { BStatement body = buildBUpdateCode(rscope.right, env, types, entities, atts); 
      Expression cond = rscope.left;
      cond.typeCheck(types,entities,context,atts);
      BStatement code = new BIfStatement(cond.binvariantForm(env,false),body,
                                         null);
      code.setWriteFrame("result"); 
      return code; 
    } 
    if (op.equals("&"))
    { Vector res = new Vector(); 
      BStatement code1 = buildBUpdateCode(rscope.left, env,types,entities,atts); 
      BStatement code2 = buildBUpdateCode(rscope.right, env,types,entities,atts); 
      if ((code1 instanceof BIfStatement) && (code2 instanceof BIfStatement))
      { BIfStatement if1 = (BIfStatement) code1; 
        BIfStatement if2 = (BIfStatement) code2; 
        if1.extendWithCase(if2); 
        return if1;
      }  
      // res.add(code1); 
      // res.add(code2);
      // return BStatement.separateUpdates(res); 
    } // for or, a choice 
    return null; 
  } 
          
    

  public BOp buildBUpdateOp(Entity ent, String opname,
                            Type t, String resT, 
                            Vector entities, Vector types)
  { java.util.Map env0 = new java.util.HashMap();
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    BExpression exbe = new BBasicExpression(ex); 
    if (instanceScope) 
    { env0.put(ename,exbe); }
    String es = ename.toLowerCase() + "s";
    BExpression esbe = new BBasicExpression(es); 

    Vector pars = getParameterNames();
    if (instanceScope) 
    { pars.add(0,ex); } 

    Vector context = new Vector(); 
    if (ent != null) 
    { context.add(ent); } 

    Expression presb; 
    if (pre == null)
    { presb = new BasicExpression("true"); } 
    else 
    { presb = pre.substituteEq("self", new BasicExpression(ex)); } 

    BExpression pre0 = getBParameterDec(); 
    BExpression pre1 = 
      BBinaryExpression.simplify("&",pre0,presb.binvariantForm(env0,false),true); 

    if ("true".equals(post + ""))
    { if (activity != null)
      { Vector localatts = (Vector) parameters.clone(); 
        activity.typeCheck(types,entities,context,localatts);
        // replace "self" by ex? 
        BStatement bactivity = activity.bupdateForm(env0,true); 
        return new BOp(opname,null,pars,pre1,bactivity);  
      } 
    } 

    // type check pre? 
    BExpression inst = new BBinaryExpression(":",exbe,esbe); 
    if (instanceScope)
    { pre1 = BBinaryExpression.simplify("&",inst,pre1,false); }
    Vector atts = (Vector) parameters.clone(); 
    post.typeCheck(types,entities,context,atts);
    if (stereotypes.contains("explicit"))
    { return explicitBUpdateOp(ent,opname,t,resT,entities,types,env0,exbe,esbe,pre1); } 

    Vector updates = post.updateVariables(); // take account of ent,
                                             // ie, like updateFeature
                                             // Assume just identifiers 4 now
    // check that all update vars are features of ent: 
    Vector preterms = post.allPreTerms(); 
    Vector entfeatures = ent.allDefinedFeatures(); // allFeatures?
    Vector pres = usedInPreForm(entfeatures,preterms); 
    updates = VectorUtil.union(updates,pres); 
    if (updates.size() == 0)
    { System.err.println("ERROR: Cannot determine write frame of " + this); 
      updates = post.allFeatureUses(entfeatures);
      System.err.println("ERROR: Assuming write frame is: " + updates); 
    } 

    BExpression dec = null;
    BParallelStatement body = new BParallelStatement();
    Vector vars = new Vector(); 

    Expression newpost = (Expression) post.clone();

    for (int i = 0; i < updates.size(); i++)
    { BasicExpression var = (BasicExpression) updates.get(i);
      if (entfeatures.contains(var.data)) 
      { BExpression oldbe = new BBasicExpression(var.data); 
        Type typ = var.getType();
        String new_var = "new_" + var.data;
        if (vars.contains(new_var)) { continue; } 
        vars.add(new_var); 
        Attribute att = new Attribute(new_var,typ,ModelElement.INTERNAL);
        att.setEntity(ent); // ?

        atts.add(att); 
        String btyp = typ.generateB(var);
        // If a set type, should be FIN(elementType.generateB())

        BExpression varbe = new BBasicExpression(new_var);
        BExpression typebe =
          new BBinaryExpression("-->",esbe,new BBasicExpression(btyp));
      
        BExpression new_dec = new BBinaryExpression(":",varbe,typebe);
        Expression newbe = new BasicExpression(new_var); 
        newbe.entity = var.entity; 
        newbe.type = var.type; 
        newbe.elementType = var.elementType; 
        newbe.umlkind = var.umlkind; 
        newbe.multiplicity = var.multiplicity; 
        dec = BBinaryExpression.simplify("&",dec,new_dec,true); 
        newpost = newpost.substituteEq(var.data,newbe);
        // should only substitute for var itself, not var@pre
        // and assign var := new_var;
        BAssignStatement stat = new BAssignStatement(oldbe,varbe); 
        body.addStatement(stat); 
      }
      else 
      { System.err.println("ERROR: " + var.data + " is not a feature of " + ent); }
    }

    BExpression pred = newpost.binvariantForm(env0,false);      
    pred = BBinaryExpression.simplify("&",dec,pred,false);
    BStatement code; 
    if (vars.size() > 0)
    { code = new BAnyStatement(vars,pred,body); }
    else
    { code = body; }  // skip
    return new BOp(opname,null,pars,pre1,code);  
  }

  public BOp explicitBUpdateOp(Entity ent, String opname,
                            Type t, String resT, 
                            Vector entities, Vector types,java.util.Map env0,
                            BExpression exbe, BExpression esbe, BExpression pre1)
  { BStatement stat = post.bOpupdateForm(env0,true); 
    Vector pars = getParameterNames();
    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    if (instanceScope) 
    { pars.add(0,ex); } 
    return new BOp(opname,null,pars,pre1,stat); 
  }  

  public BOp buildGlobalBUpdateOp(Entity ent, String opname,
                            Type t, String resT, 
                            Vector entities, Vector types, Vector constraints)
  { java.util.Map env0 = new java.util.HashMap();
    if (ent == null) 
    { return null; } 

    String ename = ent.getName(); 
    String ex = ename.toLowerCase() + "x"; 
    BExpression exbe = new BBasicExpression(ex); 
    env0.put(ename,exbe);
    String es = ename.toLowerCase() + "s";
    BExpression esbe = new BBasicExpression(es); 

    Vector context = new Vector(); 
    context.add(ent); 

    // type check pre? 
    Expression presb;
    if (pre == null) 
    { presb = new BasicExpression("true"); } 
    else 
    { presb = pre.substituteEq("self", new BasicExpression(ex)); } 

    BExpression pre0 = getBParameterDec(); 
    BExpression pre1 = 
      BBinaryExpression.simplify("&",pre0,presb.binvariantForm(env0,false),true); 
    BExpression inst = new BBinaryExpression(":",exbe,esbe); 
    pre1 = BBinaryExpression.simplify("&",inst,pre1,false);
    Vector atts = (Vector) parameters.clone(); 
    // are these used? 

    Vector pars = getParameterNames();
    pars.add(0,ex); 
    int count = 0; 

    BParallelStatement body = new BParallelStatement();
     
    BOperationCall call = new BOperationCall(opname,pars);
    body.addStatement(call);
    
    for (int i = 0; i < constraints.size(); i++) 
    { Constraint cc = (Constraint) constraints.get(i); 
      Constraint ccnew = cc.matchesOp(opname,this); 
      if (ccnew != null)
      { boolean ok = ccnew.typeCheck(types,entities,context); 
        if (ok)
        { BStatement upd = ccnew.bupdateOperation(ent,false); 
          body.addStatement(upd);
          count++;  
        } 
      } 
    } // B does not allow 2 ops of same machine to be called in 
      // parallel!
    
    if (count == 0)   
    { return new BOp(opname + "_" + ename,null,pars,pre1,call); }
    return new BOp(opname + "_" + ename,null,pars,pre1,body); 
    // This operation must ensure the preconditions of ops that it calls. 
  }

  private Vector usedInPreForm(Vector features, Vector preForms)
  { Vector res = new Vector(); 
    for (int i = 0; i < preForms.size(); i++) 
    { BasicExpression pf = (BasicExpression) preForms.get(i); 
      if (features.contains(pf.data))
      { BasicExpression var = (BasicExpression) pf.clone(); 
        var.setPrestate(false); 
        res.add(var);
      } 
    } 
    return res; 
  } 

  public Vector operationsUsedIn()
  { Vector res = new Vector(); 
    if (pre == null) { } 
    else 
    { res.addAll(pre.allOperationsUsedIn()); } 

    if (activity != null)
    { res.addAll(activity.allOperationsUsedIn()); 
      return res; 
    } 
    
    if (post == null) 
    { return res; } 
    
    res.addAll(post.allOperationsUsedIn()); 
    return res; 
  } 

  public int efo() 
  { Vector clls = operationsUsedIn(); 
    java.util.Set st = new HashSet(); 
    for (int i = 0; i < clls.size(); i++) 
    { st.add(clls.get(i)); } 
    return st.size(); 
  } 

  public void getCallGraph(Map res)
  { Vector bfcalls = operationsUsedIn(); 
    for (int j = 0; j < bfcalls.size(); j++) 
    { res.add_pair(name, bfcalls.get(j)); } 
  } 

  public int analyseEFO(Map m) 
  { // count the number of different targets of calls from the map
    java.util.Set calls = new HashSet(); 
    for (int i = 0; i < m.elements.size(); i++) 
    { Maplet mm = (Maplet) m.get(i); 
      calls.add(mm.getTarget() + ""); 
    } 
    int res = calls.size(); 

    System.out.println("*** Operation " + getName() + " has fan-out = " + res); 
    return res; 
  } 

  private static boolean isInScope(BinaryExpression be)
  { if (be.operator.equals(":")) { return true; }
    if (be.operator.equals("&"))
    { return isInScope((BinaryExpression) be.left) &&
             isInScope((BinaryExpression) be.right);
    }
    // if (be.operator.equals("#") || be.operator.equals("#1"))
    // { return isInScope((BinaryExpression) be.right); } 
    return false;
  }

  private static boolean isEqScope(BinaryExpression be)
  { if (be.operator.equals("=")) { return true; }
    if (be.operator.equals("&"))
    { return (be.left instanceof BinaryExpression) &&
             (be.right instanceof BinaryExpression) && 
             isEqScope((BinaryExpression) be.left) &&
             isEqScope((BinaryExpression) be.right);
    }
    if (be.operator.equals("=>"))
    { return (be.right instanceof BinaryExpression) &&
             isEqScope((BinaryExpression) be.right); 
    }
    // if (be.operator.equals("#") || be.operator.equals("#1"))
    // { return isEqScope((BinaryExpression) be.right); } 
    return false;
  }  // if => then true if isEqScope right? 

  private static boolean isSubScope(BinaryExpression be)
  { if (be.operator.equals("<:")) { return true; }
    if (be.operator.equals("&"))
    { return isSubScope((BinaryExpression) be.left) &&
             isSubScope((BinaryExpression) be.right);
    }
    // if (be.operator.equals("#") || be.operator.equals("#1"))
    // { return isSubScope((BinaryExpression) be.right); } 
    return false;
  }

  private static Expression returnValue(BinaryExpression be)
  { if (be.operator.equals("=") && "result".equals(be.left + ""))  // check result is on left
    { return be.right; }
    else if (be.operator.equals("&"))
    { if (be.left instanceof BinaryExpression)
      { Expression res = returnValue((BinaryExpression) be.left);
        if (res != null)
        { return res; }
        else if (be.right instanceof BinaryExpression)
        { return returnValue((BinaryExpression) be.right); }
        else
        { return null; }
      }
      else if (be.right instanceof BinaryExpression)
      { return returnValue((BinaryExpression) be.right); }
    }
    else if (be.operator.equals("=>") &&
             (be.right instanceof BinaryExpression))
    { return returnValue((BinaryExpression) be.right); } 
    else if (be.operator.equals("#") || be.operator.equals("#1"))
    { return returnValue((BinaryExpression) be.right); } 
    return null;
  }

  private static BExpression getBRange(BinaryExpression be,
                                       java.util.Map env)
  { if (be.operator.equals(":") || be.operator.equals("<:"))
    { return be.right.binvariantForm(env,false); }
    if (be.operator.equals("&")) // take intersection
    { BExpression lqf = getBRange((BinaryExpression) be.left,
                                  env);
      BExpression rqf = getBRange((BinaryExpression) be.right,
                                  env);
      if (("" + lqf).equals("" + rqf)) { return lqf; } 
      return new BBinaryExpression("/\\",lqf,rqf);
    }
    return null;
  }

  private static String getRange(BinaryExpression be,
                                 java.util.Map env)
  { if (be.operator.equals(":") || be.operator.equals("<:"))
    { return be.right.queryForm(env,true); }
    if (be.operator.equals("&")) // take intersection
    { String lqf = getRange((BinaryExpression) be.left,
                                  env);
      String rqf = getRange((BinaryExpression) be.right,
                                  env);

      if (lqf == null) { return rqf; } 
      if (rqf == null) { return lqf; } 
      if (("" + lqf).equals(rqf)) { return lqf; } 
      return "Set.intersection(" + lqf + "," + rqf + ")";
    }
    return null;
  }

  private static String getRangeJava6(BinaryExpression be,
                                 java.util.Map env)
  { if (be.operator.equals(":") || be.operator.equals("<:"))
    { return be.right.queryFormJava6(env,true); }
    if (be.operator.equals("&")) // take intersection
    { String lqf = getRangeJava6((BinaryExpression) be.left,
                                  env);
      String rqf = getRangeJava6((BinaryExpression) be.right,
                                  env);

      if (lqf == null) { return rqf; } 
      if (rqf == null) { return lqf; } 
      if (("" + lqf).equals(rqf)) { return lqf; } 
      return "Set.intersection(" + lqf + "," + rqf + ")";
    }
    return null;
  }

  private static String getRangeJava7(BinaryExpression be,
                                 java.util.Map env)
  { if (be.operator.equals(":") || be.operator.equals("<:"))
    { return be.right.queryFormJava7(env,true); }
    if (be.operator.equals("&")) // take intersection
    { String lqf = getRangeJava7((BinaryExpression) be.left,
                                  env);
      String rqf = getRangeJava7((BinaryExpression) be.right,
                                  env);

      if (lqf == null) { return rqf; } 
      if (rqf == null) { return lqf; } 
      if (("" + lqf).equals(rqf)) { return lqf; } 
      return "Ocl.intersection(" + lqf + "," + rqf + ")";
    }
    return null;
  }

  private static String getRangeCSharp(BinaryExpression be,
                                 java.util.Map env)
  { if (be.operator.equals(":") || be.operator.equals("<:"))
    { return be.right.queryFormCSharp(env,true); }
    if (be.operator.equals("&")) // take intersection
    { String lqf = getRangeCSharp((BinaryExpression) be.left,
                                  env);
      String rqf = getRangeCSharp((BinaryExpression) be.right,
                                  env);

      if (lqf == null) { return rqf; } 
      if (rqf == null) { return lqf; } 
      if (("" + lqf).equals(rqf)) { return lqf; } 
      return "SystemTypes.intersection(" + lqf + "," + rqf + ")";
    }
    return null;
  }

  private static String getRangeCPP(BinaryExpression be,
                                 java.util.Map env)
  { if (be.operator.equals(":") || be.operator.equals("<:"))
    { return be.right.queryFormCPP(env,true); }
    if (be.operator.equals("&")) // take intersection
    { String lqf = getRangeCPP((BinaryExpression) be.left,
                                  env);
      String rqf = getRangeCPP((BinaryExpression) be.right,
                                  env);

      if (lqf == null) { return rqf; } 
      if (rqf == null) { return lqf; } 
      if (("" + lqf).equals(rqf)) { return lqf; } 
      String lcet = be.left.elementType.getCPP(); 
      return "UmlRsdsLib<" + lcet + ">::intersection(" + lqf + "," + rqf + ")";
    }
    return null;
  }

  private static Type getElementType(BinaryExpression be)
  { if (be.operator.equals("<:"))
    { return be.right.elementType; }
    if (be.operator.equals("&")) // take intersection
    { return getElementType((BinaryExpression) be.left); }
    return null;
  }

  // Check completeness: if post updates v but not w when w data depends on v
}


