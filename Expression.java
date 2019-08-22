import java.util.Vector; 
import java.util.List; 
import java.io.*;

/******************************
* Copyright (c) 2003,2019 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/
/* package: OCL */

abstract class Expression 
{ Expression javaForm;  

  /* The modality of the expression, ie, if it 
     consists of purely sensor, purely actuator 
     or other combination of variables: */ 
  public static final int SENSOR = 0;
  public static final int ACTUATOR = 1;
  public static final int MIXED = 2;
  public static final int HASEVENT = 4; 
  public static final int ERROR = 10; 

  public int modality = ModelElement.NONE; // in uml version use SEN, ACT, etc

  public static final int UNKNOWN = -1; 
  public static final int VALUE = 0; 
  public static final int ATTRIBUTE = 1; 
  public static final int ROLE = 2; 
  public static final int VARIABLE = 3; 
  public static final int CONSTANT = 4; 
  public static final int FUNCTION = 5; 
  public static final int QUERY = 6;    // query operation
  public static final int UPDATEOP = 7; 
  public static final int CLASSID = 8;  // name of a class
  public static final int TYPE = 9; 

  protected int umlkind = UNKNOWN; 
  protected Type type = null; 
  protected Type elementType = null;  // type of elements if a collection
  protected Entity entity = null;  // entity of the elementType if a collection, value 
                                   // or variable. For a feature, the owning entity. 
  protected int multiplicity = ModelElement.ONE; 
                                   // multiplicity of the feature, if a feature
  protected boolean needsBracket = false; 
  protected boolean isStatic = false; 

  protected static Vector comparitors = new Vector(); 
  static 
  { comparitors.add("="); 
    comparitors.add("<"); 
    comparitors.add("<="); 
    comparitors.add(">");
    comparitors.add(">=");
    comparitors.add("!=");  // now it is /= 
    comparitors.add("/="); 
  } 

  protected static Vector oclops = new Vector(); 
  static 
  { oclops.add("->select"); 
    oclops.add("->forAll"); 
    oclops.add("->exists"); 
    oclops.add("->reject");
    oclops.add("->collect");
    oclops.add("->intersect"); 
    oclops.add("->union"); 
  } // and ->intersectAll, etc. Used anywhere? 

  public static Vector operators = new Vector(); 
  static 
  { operators.add("<=>"); 
    operators.add("=>"); 
    operators.add("#"); 
    operators.add("or"); 
    operators.add("&"); 
  }

  public static Vector alloperators = new Vector(); 
  static 
  { alloperators.add("=>"); 
    alloperators.add("or"); 
    alloperators.add("&"); 
    alloperators.add("="); 
    alloperators.add("<"); 
    alloperators.add("<="); 
    alloperators.add(">");
    alloperators.add(">=");
    alloperators.add("!=");  // now it is /= 
    alloperators.add("/="); 
    alloperators.add("mod"); 
    alloperators.add("+"); 
    alloperators.add("-"); 
    alloperators.add("/"); 
    alloperators.add("*"); 
    alloperators.add("\\/"); 
    alloperators.add("/\\"); 
    alloperators.add("^"); 
    alloperators.add(":"); 
    alloperators.add("/:"); 
    alloperators.add("<:"); 
    alloperators.add("/<:"); 
  }  // and? 

  public static java.util.Map oppriority = new java.util.HashMap(); 
  { oppriority.put("<=>",new Integer(0)); 
    oppriority.put("=>",new Integer(1)); 
    oppriority.put("#",new Integer(2)); 
    oppriority.put("or",new Integer(3)); 
    oppriority.put("&",new Integer(4)); 
  }  // and? 

  // For extensions:        
  public static java.util.Map extensionoperators = new java.util.HashMap(); 
  public static java.util.Map extensionopjava = new java.util.HashMap(); 
  public static java.util.Map extensionopcsharp = new java.util.HashMap(); 


  public static String negateOp(String op)
  { if (op.equals("=")) { return "/="; }
    if (op.equals("<=")) { return ">"; }
    if (op.equals("<")) { return ">="; }
    if (op.equals(">=")) { return "<"; }
    if (op.equals(">")) { return "<="; }
    if (op.equals("/=")) { return "="; }
    if (op.equals(":")) { return "/:"; }
    if (op.equals("/:")) { return ":"; }
    if (op.equals("<:")) { return "/<:"; }
    if (op.equals("/<:")) { return "<:"; }
    if (op.equals("!=")) { return "="; } 
    if (op.equals("->includes")) { return "->excludes"; } 
    if (op.equals("->excludes")) { return "->includes"; } 
    // if (op.equals("->exists")) { return "->forAll"; } 
    // if (op.equals("->forAll")) { return "->exists"; }
    // if (op.equals("#")) { return "!"; } 
    // if (op.equals("!")) { return "#"; } 
 
    return op;
  }  // Not correct for ->exists, ->forAll, #, !

  public static String invertOp(String op)
  { if (op.equals("->exists")) { return "->forAll"; } 
    if (op.equals("->forAll")) { return "->exists"; }
    if (op.equals("#")) { return "!"; } 
    if (op.equals("!")) { return "#"; } 
 
    return op;
  } // so that not(s->exists( x | P )) is s->forAll(x | not(P)), etc 

  public static String getCInputType(Type t) 
  { String tname = t.getName(); 
    if ("String".equals(tname)) { return "char"; } 
    return tname; 
  } 

  public Expression firstConjunct()
  { return this; } 

  public Expression removeFirstConjunct()
  { return this; 
    // return new BasicExpression("true"); 
  } 

  public Type getType() { return type; }

  public int getKind() { return umlkind; } 

  public void setUmlKind(int k)
  { umlkind = k; } 

  public void setEntity(Entity e)
  { entity = e; } 

  public Type getElementType() { return elementType; } 

  public void setType(Type t)
  { type = t; 
    if (type != null && type.isEntity)
    { entity = type.entity; }
  }  // No, not for features (attributes, roles, operations) 

  public void setMultiplicity(int m)
  { multiplicity = m; } 

  public void setElementType(Type t)
  { elementType = t; }

  public Entity getEntity() { return entity; } 

  public abstract void setPre(); 

  public boolean isVariable()
  { return (umlkind == VARIABLE); } 

  public boolean isFeature()
  { return (umlkind == ROLE || umlkind == ATTRIBUTE || umlkind == QUERY ||
            umlkind == UPDATEOP); 
  } 

  public boolean isSingleValued()
  { if (type == null) { return false; } 

    String tname = type.getName(); 
    if ("Set".equals(tname)) { return false; } 
    if ("Sequence".equals(tname)) { return false; } 
    return true; 
    // return multiplicity == ModelElement.ONE; }
  }  

  public boolean isMultipleValued()
  { if (type == null) { return false; } 

    String tname = type.getName(); 
    if ("Set".equals(tname)) { return true; } 
    if ("Sequence".equals(tname)) { return true; } 
    return false; 
    // return multiplicity != ModelElement.ONE;
  } 

  public static boolean isSimpleEntity(Expression ee) 
  { if (ee == null) 
    { return false; } 

    if (ee.umlkind == Expression.CLASSID && (ee instanceof BasicExpression) &&
        ((BasicExpression) ee).arrayIndex == null)
    { return true; } 
    return false; 
  }  

  public static boolean isEntity(Expression ee) 
  { if (ee == null) 
    { return false; } 

    Type te = ee.getElementType(); 
    if (Type.isEntityType(te) && (te + "").equals(ee + ""))
    { return true; } 
    
    return false; 
  } 


  public static void addOperator(String op, Type typ) 
  { extensionoperators.put(op,typ); } 

  public static void addOperatorJava(String op, String jcode)
  { extensionopjava.put(op,jcode); } 

  public static void addOperatorCSharp(String op, String cscode)
  { extensionopcsharp.put(op,cscode); } 

  public static Type getOperatorType(String op) 
  { return (Type) extensionoperators.get(op); } 

  public static String getOperatorJava(String op) 
  { return (String) extensionopjava.get(op); } 

  public static String getOperatorCSharp(String op) 
  { return (String) extensionopcsharp.get(op); } 

  public static void saveOperators(PrintWriter out)
  { java.util.Iterator keys = extensionoperators.keySet().iterator();
    while (keys.hasNext())
    { Object k = keys.next();
      String opname = (String) k;  
      String typ = extensionoperators.get(k) + ""; 
      out.println("Operator:"); 
      out.println(opname + " " + typ); 
      String opjava = (String) extensionopjava.get(k);
      if (opjava != null) 
      { out.println(opjava); }
      else 
      { out.println(); }   
      String opcsharp = (String) extensionopcsharp.get(k);
      if (opcsharp != null) 
      { out.println(opcsharp); }
      else 
      { out.println(); }   
    } 
  } 

  public abstract Expression definedness(); 

  public abstract Expression determinate(); 

  public static Statement iterationLoop(Expression var, Expression range, Statement body)
  { BinaryExpression test = new BinaryExpression(":", var, range);
    WhileStatement ws = new WhileStatement(test, body);
    ws.setLoopKind(Statement.FOR);
    ws.setLoopRange(var,range);
    return ws;
  }

  public String findEntityVariable(java.util.Map env)
  { if (entity == null)
    { return ""; } 
    String nme = entity + ""; 
    String var = (String) env.get(nme);
    if (var == null)  // because nme is superclass of ent: dom(env)
    { var = entity.searchForSubclassJava(env);  
      if (var == null)
      { var = entity.searchForSuperclassJava(env); 
        if (var != null) 
        { var = "((" + nme + ") " + var + ")"; }  
        else 
        { System.err.println("** Specification error: no variable of " + entity); 
          var = nme.toLowerCase() + "x"; 
        } 
      }
      return var; 
    }
    return var; 
  } 
 
  static Vector caselist(Expression e)
  { if (e == null) { return new Vector(); }

    if (e instanceof BinaryExpression)
    { BinaryExpression be = (BinaryExpression) e;
      if ("&".equals(be.operator))
      { Vector v1 = caselist(be.left);
        Vector v2 = caselist(be.right);
        Vector res = new Vector();
        res.addAll(v1); res.addAll(v2);
        return res;
      }
    }
    Vector v = new Vector();
    v.add(e);
    return v;
  }

  public String saveModelData(PrintWriter out) { return "_"; } 

  public String unwrap(String se)
  { if (type == null)
    { return se; }  // should never happen
    String tname = type.getName();
    if (tname.equals("double"))
    { return "((Double) " + se + ").doubleValue()"; } 
    if (tname.equals("boolean"))
    { return "((Boolean) " + se + ").booleanValue()"; } 
    if (tname.equals("int"))
    { return "((Integer) " + se + ").intValue()"; }
    if (tname.equals("long"))
    { return "((Long) " + se + ").longValue()"; }
    if (type.isEnumerated())
    { return "((Integer) " + se + ").intValue()"; } // for enumerated
    return se;   // can't unwrap -- for strings or objects
  }

  public static String unwrap(String se, Type typ)
  { if (typ == null)
    { return se; }  // should never happen
    String tname = typ.getName();
    if (tname.equals("double"))
    { return "((Double) " + se + ").doubleValue()"; } 
    if (tname.equals("boolean"))
    { return "((Boolean) " + se + ").booleanValue()"; } 
    if (tname.equals("int"))
    { return "((Integer) " + se + ").intValue()"; }
    if (tname.equals("long"))
    { return "((Long) " + se + ").longValue()"; }
    if (typ.isEnumerated())
    { return "((Integer) " + se + ").intValue()"; } // for enumerated
    if (tname.equals("Set") || tname.equals("Sequence"))
    { return se; }  // "(Vector) " + se
    return "(" + tname + ") " + se;   // for strings or objects
  }

  public static String convertCSharp(String se, Type t)
  { if (t == null)
    { return se; }  // should never happen
    String tname = t.getName();
    if (tname.equals("double"))
    { return "double.Parse(" + se + ")"; } 
    if (tname.equals("boolean"))
    { return "bool.Parse(" + se + ")"; } 
    if (tname.equals("int"))
    { return "int.Parse(" + se + ")"; }
    if (tname.equals("long"))
    { return "Int64.convert(" + se + ")"; }
    if (t.isEnumerated())
    { return "int.Parse(" + se + ")"; } // for enumerated
    return se;   // can't unwrap -- for strings or objects
  }



  public String unwrapCSharp(String se)
  { if (type == null)
    { return se; }  // should never happen
    String tname = type.getName();
    if (tname.equals("double"))
    { return "((double) " + se + ")"; } 
    if (tname.equals("boolean"))
    { return "((bool) " + se + ")"; } 
    if (tname.equals("int"))
    { return "((int) " + se + ")"; }
    if (tname.equals("long"))
    { return "((long) " + se + ")"; }
    if (type.isEnumerated())
    { return "((int) " + se + ")"; } // for enumerated
    return se;   // can't unwrap -- for strings or objects
  }

  public abstract Expression skolemize(Expression sourceVar, java.util.Map env); 

  public Vector topLevelSplit(String d)
  { // divides d into substrings using . at top level only
    int dlen = d.length();
    Vector res = new Vector();
    StringBuffer sb = new StringBuffer();
    int bcount = 0;
    int sqbcount = 0;
    int scount = 0;
    boolean instring = false;

    char prev = ' '; 

    for (int i = 0; i < dlen; i++)
    { char c = d.charAt(i);
      if (c == '.')
      { if (bcount == 0 && sqbcount == 0 && scount == 0 &&
            !instring)
        { res.add(sb.toString());
          sb = new StringBuffer();
        }
        else 
        { sb.append(c); } 
      }
      else if (c == '(') { bcount++; sb.append(c); }
      else if (c == ')') { bcount--; sb.append(c); }
      else if (c == '[') { sqbcount++; sb.append(c); }
      else if (c == ']') { sqbcount--; sb.append(c); }
      else if (c == '{') { scount++; sb.append(c); }
      else if (c == '}') { scount--; sb.append(c); }
      else if (c == '"' && prev != '\\') 
      { if (instring) { instring = false; } else { instring = true; }
        sb.append(c); 
      }
      else { sb.append(c); }
      prev = c; 
    }
    res.add(sb.toString());
    return res;
  }

  public static Expression removeExistentials(Expression e, Vector vscopes, 
                                              Vector vars)
  { if (e instanceof BinaryExpression)
    { BinaryExpression eb = (BinaryExpression) e; 
      if (eb.operator.equals("#") || eb.operator.equals("#1"))
      { BinaryExpression sleft = (BinaryExpression) eb.left; 
        BasicExpression var = (BasicExpression) sleft.left;
        vars.add(var + "");  
        vscopes.add(sleft); 
        Expression entB = eb.right;
        return removeExistentials(entB,vscopes,vars); 
      } 
      else if (eb.operator.equals("&")) // E1->exists( e1 | expr1 & E2->exists( e2 | expr2 ))
      { Expression res = removeExistentials(eb.right, vscopes, vars); 
        return simplifyAnd(eb.left, res); 
      } 
      return e; 
    } 
    
    return e; 
  } 

  public static Expression conjoin(Vector exps)
  { if (exps.size() == 0) { return new BasicExpression(true); } 
    if (exps.size() == 1) { return (Expression) exps.get(0); } 
    Vector tail = new Vector(); 
    Expression head = (Expression) exps.get(0);
    tail.addAll(exps); 
    tail.remove(head);  
    return simplifyAnd(head,conjoin(tail)); 
  } 
        
  public String updateForm(String language, java.util.Map env, boolean local)
  { if ("Java4".equals(language))
    { return updateForm(env,local); } 
    else if ("Java6".equals(language))
    { return updateFormJava6(env,local); } 
    else if ("Java7".equals(language))
    { return updateFormJava7(env,local); } 
    else if ("CSharp".equals(language))
    { return updateFormCSharp(env,local); } 
    else 
    { return updateFormCPP(env,local); } 
  } 

  public String updateForm(String language, java.util.Map env, 
                                    String op, String val, Expression var, boolean local)
  { return ""; }  

  public abstract void findClones(java.util.Map clones, String rule, String op); 

  public abstract String updateForm(java.util.Map env, boolean local); 

  public Statement generateDesign(java.util.Map env, boolean local)
  { return new SequenceStatement(); } 

  public Statement statLC(java.util.Map env, boolean local)
  { return generateDesign(env,local); } 

  public boolean isExecutable()
  { return false; } 

  public boolean isTestable()
  { return true; } 

  public String updateFormCSharp(java.util.Map env, boolean local)
  { return updateForm(env,local); }  

  public String updateFormJava6(java.util.Map env, boolean local)
  { return updateForm(env,local); }  

  public String updateFormJava7(java.util.Map env, boolean local)
  { return updateForm(env,local); }  

  public String updateFormCPP(java.util.Map env, boolean local)
  { return updateForm(env,local); }  

  public static String javaOpOf(String op)
  { if (op.equals("&") || op.equals("and")) { return "&&"; }
    if (op.equals("or")) { return "||"; }
    if (op.equals("=")) { return "=="; }
    if (op.equals("/=")) { return "!="; }
    if (op.equals("mod")) { return "%"; }
    return op;
  }

  public Vector writeFrame()
  { return new Vector(); }   // default

  public Scope resultScope()
  { return null; }

  public boolean isUpdateable() { return false; }   // default
 
  public static boolean toBoolean(String s)
  { if (s == null) { return false; }
    if (s.equals("true")) { return true; }
    return false; 
  } 

  public boolean isOrdered()
  { return false; } // default

  public boolean isSorted()
  { return false; } // default

  public boolean isOrderedB()
  { return false; } // default

  public abstract Vector allEntitiesUsedIn(); 

  public abstract Vector innermostEntities(); // cases of SetExpression etc needed

  public abstract Vector innermostVariables(); 
  // cases of SetExpression etc needed

  public abstract Vector allPreTerms();

  public abstract Vector allPreTerms(String var);

  public abstract Expression addPreForms(String var); 

  public Vector getBaseEntityUses()
  { return new Vector(); } // default
 

  public abstract Vector getVariableUses(); 

  public Vector allFeatureUses(Vector features)
  { Vector res = new Vector(); 
    for (int i = 0; i < features.size(); i++) 
    { res.addAll(getUses((String) features.get(i))); } 
    return res; 
  } 

  public abstract Vector getUses(String feature); 

  public abstract Vector allFeaturesUsedIn(); 

  public abstract Vector allOperationsUsedIn(); 

  public abstract Vector equivalentsUsedIn(); 

  public abstract Vector allValuesUsedIn(); 

  public abstract Vector allBinarySubexpressions(); 

  public Vector updateVariables() { return new Vector(); } 

  public Vector allConjuncts()
  { return new Vector(); }

  public Vector allDisjuncts()
  { Vector res = new Vector();
    res.add(this); 
    return res; 
  }

  public Vector getConjuncts()
  { Vector res = new Vector();
    res.add(this); 
    return res; 
  }

  public Expression preconditionExpression()  // for Java
  { Vector conjs = computeNegation4ante();
    Expression disj;
    if (conjs.size() == 0)
    { return new BasicExpression("false"); }  // ???
    disj = (Expression) conjs.get(0);
    for (int i = 1; i < conjs.size(); i++)
    { Expression c = (Expression) conjs.get(i);
      disj = Expression.simplify("or",disj,c,false);
    }
    return disj;
  }

  public abstract boolean relevantOccurrence(String op, 
                                             Entity ent, String val, String f); 

  public abstract DataDependency getDataItems(); 

  public abstract DataDependency rhsDataDependency(); 

  public abstract Expression addReference(BasicExpression ref, Type t);

  public abstract Expression replaceReference(BasicExpression ref, Type t);

  public abstract Expression dereference(BasicExpression ref);

  public abstract Expression invert(); 

  public String wrap(String qf)
  { return wrap(type,qf); }

  public static String wrap(Type t, String qf)
  { if (t != null)
    { if (t.isEntity()) { return qf; } 
      String tname = t.getName();
      if (tname.equals("Set") || tname.equals("Sequence") || tname.equals("String"))
      { return qf; }
      if (tname.equals("boolean"))
      { return "new Boolean(" + qf + ")"; }
      else if (tname.equals("double"))
      { return "new Double(" + qf + ")"; }
      else if (tname.equals("long"))
      { return "new Long(" + qf + ")"; }
      else
      { return "new Integer(" + qf + ")"; }
    }
    else // already an object
    { return qf; }
  }

  public String wrapCSharp(String qf)
  { return wrapCSharp(type,qf); }

  public static String wrapCSharp(Type t, String qf)
  { if (t != null)
    { if (t.isEntity()) { return qf; } 
      String tname = t.getName();
      if (tname.equals("Set") || tname.equals("Sequence") || tname.equals("String"))
      { return qf; }
      if (tname.equals("boolean"))
      { return "((bool) " + qf + ")"; }
      else if (tname.equals("double"))
      { return "((double) " + qf + ")"; }
      else if (tname.equals("long"))
      { return "((long) " + qf + ")"; }
      else
      { return "((int) " + qf + ")"; }
    }
    else // already an object
    { return qf; }
  }

  public String makeSet(String qf)
  { String obj = qf; 
    // if (isPrimitive())
    // { obj = wrap(qf); } 
    // else 
    // { obj = qf; } 

    return "(new Set()).add(" + obj + ").getElements()";
  }

  public String makeSetJava6(String qf)
  { String obj = qf; 
    // if (isPrimitive())
    // { obj = wrap(qf); } 
    // else 
    // { obj = qf; } 

    return "Set.addSet(new HashSet(), " + obj + ")";
  }

  public String makeSequenceJava6(String qf)
  { String obj = qf; 
    // if (isPrimitive())
    // { obj = wrap(qf); } 
    // else 
    // { obj = qf; } 

    return "Set.addSequence(new ArrayList(), " + obj + ")";
  }  // I assume? 

  public String makeSetJava7(String qf)
  { String obj = qf; 
    // if (isPrimitive())
    // { obj = wrap(qf); } 
    // else 
    // { obj = qf; } 
    Type seqtype = new Type("Set",null); 
    seqtype.setElementType(type); 
    String jtype = seqtype.getJava7(type); 

    return "Ocl.addSet(new " + jtype + "(), " + obj + ")";
  }

  public String makeSequenceJava7(String qf)
  { String obj = qf; 
    // if (isPrimitive())
    // { obj = wrap(qf); } 
    // else 
    // { obj = qf; } 
    Type seqtype = new Type("Sequence",null); 
    seqtype.setElementType(type); 
    String jtype = seqtype.getJava7(type); 

    return "Ocl.addSequence(new " + jtype + "(), " + obj + ")";
  }  // I assume? 

  public String makeSetCSharp(String qf)
  { String obj = qf; 

    return "SystemTypes.makeSet(" + obj + ")";
  }

  public String makeSetCPP(String qf)
  { String obj = qf; 

    String cet = "void*"; 
    if (elementType != null) 
    { cet = elementType.getCPP(); } 
    
    return "UmlRsdsLib<" + cet + ">::makeSet(" + obj + ")";
  }

  public String makeSequenceCPP(String qf)
  { String obj = qf; 

    String cet = "void*"; 
    if (elementType != null) 
    { cet = elementType.getCPP(); } 
    
    return "UmlRsdsLib<" + cet + ">::makeSequence(" + obj + ")";
  }

  public abstract boolean isPrimitive(); 

  public boolean isAssignable()
  { return false; } // default

  public Expression checkConversions(Type typ, Type elemTyp, java.util.Map interp) 
  { return this; }   // default 

  public Expression replaceModuleReferences(UseCase uc) 
  { return this; }   // default 


  public abstract int typeCheck(final Vector sms);

  public boolean typeCheck(final Vector typs, final Vector ents,
                                    final Vector env)
  { Vector contexts = new Vector(); 
    if (entity != null)
    { contexts.add(entity); }  // For features, only 
    return typeCheck(typs,ents,contexts,env); 
  }  
     

  public abstract boolean typeCheck(final Vector typs, final Vector ents,
                                    final Vector contexts, final Vector env); 

  public abstract int maxModality();

  public abstract int minModality(); 

  public static boolean isNumber(Object ob) 
  { try
    { double nn = Double.parseDouble("" + ob); 
      return true; 
    } 
    catch (Exception e) 
    { try 
      { long ll = Long.parseLong("" + ob); 
        return true; 
      } 
      catch (Exception ee)
      { return false; }
    }  
  } 

  public static boolean isInteger(Object ob) 
  { try
    { int nn = Integer.parseInt("" + ob); 
      return true; 
    } 
    catch (Exception e) 
    { return false; } 
  } 

  public static boolean isLong(Object ob) 
  { try
    { long nn = Long.parseLong("" + ob); 
      return true; 
    } 
    catch (Exception e) 
    { return false; } 
  } 

  public static boolean isDouble(Object ob) 
  { try
    { double nn = Double.parseDouble("" + ob); 
      return true; 
    } 
    catch (Exception e) 
    { return false; } 
  } 

  public static boolean isValue(Object ob)
  { if (isNumber(ob)) { return true; } 
    if (ob instanceof String) 
    { if (isString((String) ob)) { return true; } 
      if (isBoolean((String) ob)) { return true; }
    }  
    return false; 
  } // and values of enum types? 

  public static boolean isConstant(String s)
  { // it is all capitals
    if (s == null || s.length() == 0) { return false; } 
    return (s.toUpperCase().equals(s)); 
  } 

  public static boolean isString(String data)
  { int len = data.length();
    if (len > 1 &&
        data.charAt(0) == '\"' &&
        data.charAt(len-1) == '\"')
    { return true; }
    return false;
  }

  public static boolean isBoolean(String data)
  { return data.equals("true") || data.equals("false"); }

  public static boolean isSet(String data)
  { int len = data.length();
    if (len > 1 && data.charAt(0) == '{' && 
        data.charAt(len-1) == '}')
    { return true; }
    if (len > 4 && data.substring(0,4).equals("Set{") && 
        data.charAt(len-1) == '}')
    { return true; }
    return false;
  }

  public static boolean isSequence(String data)
  { int len = data.length();
    if (len > 9 && data.substring(0,9).equals("Sequence{") &&
        data.charAt(len-1) == '}')
    { return true; }
    return false;
  }

  public boolean isCall(String data)
  { int n = data.length();
    int i = data.indexOf('('); 
    if (n > 0 &&
        data.charAt(n-1) == ')' &&
        i > 0)
    { return true; }
    return false; 
  }


  public static boolean isFunction(String d)
  { if (d.equals("sqr") || d.equals("sqrt") || d.equals("any") || d.equals("cbrt") || 
        d.equals("sin") || d.equals("cos") || d.equals("tan") || d.equals("exp") ||
        d.equals("sinh") || d.equals("cosh") || d.equals("tanh") || d.equals("log10") ||
        d.equals("log") || d.equals("floor") || d.equals("round") || d.equals("ceil") || 
        d.equals("atan") || d.equals("acos") || d.equals("asin") || d.equals("oclAsType") || 
        d.equals("abs") || d.equals("max") || d.equals("subcollections") || d.equals("Prd") || 
        d.equals("size") || d.equals("toLowerCase") || d.equals("pow") || d.equals("Sum") ||
        d.equals("toUpperCase") || d.equals("closure") || d.equals("asSet") || d.equals("asSequence") ||
        d.equals("min") || d.equals("sum") || d.equals("reverse") || d.equals("allInstances") || 
        d.equals("sort") || d.equals("prd") || d.equals("last") || d.equals("insertAt") ||
        d.equals("first") || d.equals("tail") || d.equals("front") || d.equals("oclIsUndefined") || 
        d.equals("subrange") || d.equals("indexOf") || d.equals("count") ||
        d.equals("characters") || d.equals("isDeleted") || 
        d.equals("oclIsKindOf") || d.equals("oclIsTypeOf"))
    { return true; }  
    return false;
  } // oclAsType should be BinaryExpression??? flatten??

  public boolean isRuleCall(Vector rules)
  { return false; } 

  public abstract boolean isMultiple(); 

  public abstract boolean isOrExpression(); 

  public void setBrackets(boolean brack)
  { needsBracket = brack; } 

  public Expression completeness(Entity ent)
  { Expression res = new BasicExpression("true",0);
    return res;
  }

  public static String evaluateString(String op, String le, String re) 
  { if ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op))
    { int li; 
      int ri;
      long ll; 
      long rl;  
      double ld; 
      double rd; 
      try
      { li = Integer.parseInt(le); 
        try 
        { ri = Integer.parseInt(re); 
          return "" + evaluateInteger(op,li,ri);
        }
        catch (Exception e1) 
        { try 
          { rl = Long.parseLong(re); 
            return "" + evaluateLong(op,li,rl); 
          } 
          catch (Exception e2)
          { try 
            { rd = Double.parseDouble(re); 
              return "" + evaluateDouble(op,li,rd); 
            } 
            catch (Exception e3)
            { return le + " " + op + " " + re; } 
          } 
        }  
      } catch (Exception e) { } 
      
      try 
      { ll = Long.parseLong(le); 
        rl = Long.parseLong(re); 
        return "" + evaluateLong(op,ll,rl); 
      } 
      catch (Exception e4)
      { try 
        { rd = Double.parseDouble(re); 
          ld = Double.parseDouble(le); 
          return "" + evaluateDouble(op,ld,rd); 
        } 
        catch (Exception e5)
        { return le + " " + op + " " + re; } 
      }
    } 
    return le + " " + op + " " + re;
  }       

  public static int evaluateInteger(String op, int li, int ri)
  { if ("+".equals(op))
    { return li + ri; } 
    if ("-".equals(op))
    { return li - ri; } 
    if ("*".equals(op))
    { return li * ri; } 
    if ("/".equals(op))
    { return li / ri; } 
    if ("mod".equals(op))
    { return li % ri; } 
    return 0; 
  } 

  public static long evaluateLong(String op, long li, long ri)
  { if ("+".equals(op))
    { return li + ri; } 
    if ("-".equals(op))
    { return li - ri; } 
    if ("*".equals(op))
    { return li * ri; } 
    if ("/".equals(op))
    { return li / ri; } 
    if ("mod".equals(op))
    { return li % ri; } 
    return 0; 
  } 

  public static double evaluateDouble(String op, double li, double ri)
  { if ("+".equals(op))
    { return li + ri; } 
    if ("-".equals(op))
    { return li - ri; } 
    if ("*".equals(op))
    { return li * ri; } 
    if ("/".equals(op))
    { return li / ri; } 
    return 0; 
  } 

  abstract public Expression createActionForm(final Vector v);

  public String classqueryForm(java.util.Map env, boolean local)
  { return queryForm(env,local); }   // default  

  public String classqueryFormJava6(java.util.Map env, boolean local)
  { return queryFormJava6(env,local); }   // default  

  public String classqueryFormJava7(java.util.Map env, boolean local)
  { return queryFormJava7(env,local); }   // default  

  public String classqueryFormCSharp(java.util.Map env, boolean local)
  { return queryFormCSharp(env,local); }   // default  

  public String classqueryFormCPP(java.util.Map env, boolean local)
  { return queryFormCPP(env,local); }   // default  

  abstract public String queryForm(java.util.Map env, boolean local); 

  public String queryForm(String language, java.util.Map env, boolean local)
  { if ("Java4".equals(language))
    { return queryForm(env,local); } 
    else if ("Java6".equals(language))
    { return queryFormJava6(env,local); } 
    else if ("Java7".equals(language))
    { return queryFormJava7(env,local); } 
    else if ("CSharp".equals(language))
    { return queryFormCSharp(env,local); } 
    else 
    { return queryFormCPP(env,local); } 
  } 

  public abstract String queryFormCSharp(java.util.Map env, boolean local); 
  // { return queryForm(env,local); }   // default 

  public abstract String queryFormJava6(java.util.Map env, boolean local); 
  // { return queryForm(env,local); }   // default 

  public abstract String queryFormJava7(java.util.Map env, boolean local); 

  public abstract String queryFormCPP(java.util.Map env, boolean local);
  // { return queryForm(env,local); }   // default 

  abstract public BExpression bqueryForm(java.util.Map env); 

  abstract public BExpression bqueryForm(); 

  abstract public BStatement bupdateForm(java.util.Map env, boolean local);

  public BStatement bOpupdateForm(java.util.Map env, boolean local)
  { return bupdateForm(env,local); } 


  public BExpression bInvariantForm(java.util.Map env,boolean local)
  { Vector uses = getVariableUses();
    Vector pars = new Vector(); // parameters from the context
    Vector variables = Constraint.variableTypeAssign(uses,pars); 
    BExpression pred = binvariantForm(env,local);   // add quantifiers for vbls
    return Constraint.addVariableQuantifiers(variables,pred);
  }

  abstract public BExpression binvariantForm(java.util.Map env,boolean local); 

  abstract public String toString();  // RSDS version of expression

  abstract public String toSQL(); 

  abstract public String toJava();   // Java version of expression

  abstract public String toB();  

  // abstract public String toOcl(); 

  public String toOcl(java.util.Map env, boolean local) 
  { return toString(); } 

  abstract public String toZ3();  

  abstract public Expression toSmv(Vector cnames);   // SMV version 

  abstract public String toImp(final Vector comps);  // B IMPLEMENTATION version

  abstract public String toJavaImp(final Vector comps); 

  public abstract Expression buildJavaForm(final Vector comps);

  public void setJavaForm(Expression e) 
  { javaForm = e; } 

  public static Expression substitute(final Expression e, 
                                      final Expression oldE,
                                      final Expression newE)
  { if (e == null) { return null; } 
    return e.substitute(oldE,newE); 
  } 

  abstract public Expression substitute(final Expression oldE,
                                        final Expression newE); 

  abstract public Expression substituteEq(final String var,
                                          final Expression val); 

  
  abstract public boolean hasVariable(final String s); 

  // public boolean hasComponent(final Statemachine sm)

  public Vector cwr(Vector assocs)
  { return wr(assocs); } 

  public Vector wr(Vector assocs)
  { return new Vector(); } 

  public Vector readFrame()
  { return new Vector(); } 

  public Vector allReadFrame()
  { return new Vector(); } 

  public Vector internalReadFrame()
  { return readFrame(); } 

  public boolean confluenceCheck(Vector iterated, Vector created)
  { return true; } 

  abstract public Vector variablesUsedIn(final Vector varNames);

  abstract public Vector componentsUsedIn(final Vector sms); 

  abstract Maplet findSubexp(final String var); 

  abstract public Expression simplify(final Vector vars); 

  abstract public Expression simplify(); 

  protected static Expression pruneDuplicates(Vector cnames, 
                                              Expression e1,
                                              Expression e2)
  { Vector vars1 =
      e1.variablesUsedIn(cnames);
    Vector vars2 =
      e2.variablesUsedIn(cnames);
    Vector inter =
      VectorUtil.intersection(vars1,vars2); 
    if (inter.size() == 0)
    { return simplify("&",e1,e2,null); }
    else  /* inter.size() == 1 */
    { String var = (String) inter.get(0);
      Maplet mm1 = e1.findSubexp(var);
      Maplet mm2 = e2.findSubexp(var);
      Expression sub1 = (Expression) mm1.source;
      Expression sub2 = (Expression) mm2.source;
      Expression sub = resolve(var,sub1,sub2);
      Expression rem = 
          simplify("&", (Expression) mm1.dest, 
                        (Expression) mm2.dest, null);
      return simplify("&",sub,rem,null); 
    }
  }     

  private static Expression resolve(String var,
                                    Expression e1,
                                    Expression e2)
  { if (e1 == null || e2 == null)
    { return null; }  /* Should never happen */
    else 
    { if (e1.subformulaOf(e2))
      { System.err.println("Two copies of formula " +
                    e1.toString() + " being merged"); 
        return e2;  // e2 not e1, surely?  
      }
      else 
      { System.err.println("Inconsistent formulae " +
                    e1.toString() + " and " + 
                    e2.toString() + " being removed"); 
        return new BasicExpression("false"); 
      }
    }
  }

  static public Expression simplify(final String op, 
                            final Expression e1, 
                            final Expression e2,
                            final Vector vars)
  { if (e1 == null)  { return e2; }
    if (e2 == null)  { return e1; }
    if (op.equals("#&")) { return simplifyExistsAnd(e1,e2); } 
    if (op.equals("&")) { return simplifyAnd(e1,e2); } 
    if (op.equals("or")) { return simplifyOr(e1,e2); }
    if (op.equals("=>")) { return simplifyImp(e1,e2); } 
    if (op.equals("=")) { return simplifyEq(e1,e2,vars); }
    if (op.equals("!=") || op.equals("/=")) { return simplifyNeq(e1,e2,vars); } 
    if (op.equals(":")) { return simplifyIn(e1,e2,vars); }
    if (comparitors.contains(op)) { return simplifyIneq(op,e1,e2); } 
    return new BinaryExpression(op,e1,e2);
  }

  // should extend to deal with evaluation of +, *, -, /, :, etc
  static public Expression simplify(final String op, 
                            final Expression e1, 
                            final Expression e2, boolean needsBrackets)
  { if (e1 == null)  { return e2; }
    if (e2 == null)  { return e1; }
    Expression res; 
    if (op.equals("&")) { res = simplifyAnd(e1,e2); } 
    else if (op.equals("or")) { res = simplifyOr(e1,e2); }
    else if (op.equals("=>")) { res = simplifyImp(e1,e2); } 
    else if (op.equals("=")) { res = simplifyEq(e1,e2); }
    else if (op.equals("!=") || op.equals("/=")) { res = simplifyNeq(e1,e2); } 
    else if (op.equals(":")) { res = simplifyIn(e1,e2); } 
    else if (comparitors.contains(op)) { res = simplifyIneq(op,e1,e2); } 
    else { res = new BinaryExpression(op,e1,e2); } 
    res.setBrackets(needsBrackets); 
    return res; 
  }

  public static List simplifyAnd(final List e1s, final List e2s) 
  { if (e1s == null) { return e2s; } 
    if (e2s == null) { return e1s; } 
    if (e1s.size() == 0) { return e2s; } 
    if (e2s.size() == 0) { return e1s; } 

    Expression e1 = (Expression) e1s.get(0); 
    Expression e2 = (Expression) e2s.get(0); 
    Expression e = simplifyAnd(e1,e2); 
    Vector res = new Vector(); 
    res.add(e); 
    return res; 
  }  

  public static Expression simplifyAnd(final Expression e1,
                                       final Expression e2)
  { if (e1 == null) { return e2; } 
    if (e2 == null) { return e1; } 
    if (e1.equalsString("false"))
    { return new BasicExpression("false"); } 
    if (e2.equalsString("false"))
    { return new BasicExpression("false"); } 
    if (e1.equalsString("true")) 
    { return e2; } 
    if (e2.equalsString("true"))
    { return e1; }
    if (e1.equals(e2))
    { return e1; }
    if (e1 instanceof BinaryExpression) 
    { BinaryExpression be1 = (BinaryExpression) e1; 
      if (be1.operator.equals("&"))
      { if ((e2 + "").equals(be1.left + ""))
        { return e1; } 
        else if ((e2 + "").equals(be1.right + ""))
        { return e1; } 
      } 
    }  
    if (e2 instanceof BinaryExpression) 
    { BinaryExpression be2 = (BinaryExpression) e2; 
      if (be2.operator.equals("&"))
      { if ((e1 + "").equals(be2.left + ""))
        { return e2; } 
        else if ((e1 + "").equals(be2.right + ""))
        { return e2; } 
      } 
    }  
    if (e1.conflictsWith(e2))
    { return new BasicExpression("false"); } 
    return new BinaryExpression("&",e1,e2);  
  }   // if (e1.subformulaOf(e2)) { return e2; } 

  public static Expression simplifyExistsAnd(final Expression e1,
                                       final Expression e2)
  { if (e1 instanceof BinaryExpression) 
    { BinaryExpression be1 = (BinaryExpression) e1; 
      if ("#".equals(be1.operator))
      { Expression res1 = simplifyExistsAnd(be1.right, e2); 
        return new BinaryExpression("#", be1.left, res1); 
      }
      else if ("&".equals(be1.operator))
      { Expression res1 = simplifyExistsAnd(be1.right, e2); 
        return simplifyExistsAnd(be1.left, res1); 
      }   
    } 
    return simplifyAnd(e1,e2); 
  }
  
  public static Expression simplifyOr(final Expression e1,
                                      final Expression e2)
  { if (e1.equalsString("true") || e2.equalsString("true"))
    { return new BasicExpression("true"); }
    if (e1.equalsString("false"))
    { return e2; }
    if (e2.equalsString("false"))
    { return e1; }
    if (e1.equals(e2))
    { return e1; }
    return new BinaryExpression("or",e1,e2); 
  }  // if (e1.subformulaOf(e2)) { return e1; }


  public static Expression simplifyImp(final Expression ante,
                                       final Expression succ)
  { if (ante.equalsString("true"))
    { return succ; }
    if (succ.equalsString("true"))
    { return new BasicExpression("true"); }
    if (ante.equals(succ))
    { return new BasicExpression("true"); }
    if (succ instanceof BinaryExpression)
    { BinaryExpression sbe = (BinaryExpression) succ;
      if (sbe.operator.equals("=>"))
      { Expression newante = simplifyAnd(ante,sbe.left); 
         // = new BinaryExpression("&",ante,sbe.left);
        return new BinaryExpression("=>",newante,sbe.right);
      }
    }
    return new BinaryExpression("=>",ante,succ);
  }

  private static Expression simplifyEq(final Expression e1,
                                       final Expression e2, 
                                       final Vector vars)
  { if (e1.equals(e2))
    { return new BasicExpression("true"); }
                   /* if different states of same component, false */
                   /*for (int i = 0; i < comps.size(); i++) 
                     { Statemachine sm = (Statemachine) comps.elementAt(i); 
                       boolean b1 = sm.hasState(e1); 
                       if (b1) 
                       { boolean b2 = sm.hasState(e2); 
                         if (b2) { return false; } 
                       } 
                     } */ 
    /* Or, if vars is list of all variables: 
    else 
    { boolean b1 = vars.contains(e1.toString()); 
      if (!b1)
      { boolean b2 = vars.contains(e2.toString()); 
        if (!b2) { return new BasicExpression("false"); } 
      } 
    } */ 
    return new BinaryExpression("=",e1,e2); 
  }  // new version: 
  // else if (e1.getKind() == VALUE && e2.getKind() == VALUE)
  // { return false; } 

  private static Expression simplifyNeq(final Expression e1,
                                        final Expression e2,
                                        final Vector vars)
  { if (e1.equals(e2))
    { return new BasicExpression("false"); }
    /* else 
    { boolean b1 = vars.contains(e1.toString());
      if (!b1)
      { boolean b2 = vars.contains(e2.toString());
        if (!b2) // they are both primitive values, not equal
        { return new BasicExpression("true"); }
      }
    } */ 
    return new BinaryExpression("!=",e1,e2);
  }

  private static Expression simplifyEq(final Expression e1,
                                       final Expression e2)
  { if (e1.equals(e2))
    { return new BasicExpression("true"); }
    // else if (e1.getKind() == VALUE && e2.getKind() == VALUE)
    // { return new BasicExpression("false"); } 
                   
    return new BinaryExpression("=",e1,e2); 
  }  

  private static Expression simplifyNeq(final Expression e1,
                                        final Expression e2)
  { if (e1.equals(e2))
    { return new BasicExpression("false"); }
    // else if (e1.getKind() == VALUE && e2.getKind() == VALUE)
    // { return new BasicExpression("true"); } 

    return new BinaryExpression("!=",e1,e2);
  }
  
  public static Expression negate(Expression e)
  { if (e instanceof BinaryExpression)
    { BinaryExpression be = (BinaryExpression) e; 
      String op = be.operator; 
      if (op.equals("&"))
      { Expression nleft = negate(be.left); 
        Expression nright = negate(be.right); 
        Expression res = new BinaryExpression("or", nleft, nright); 
        res.setBrackets(true); 
        return res; 
      } 
      else if (op.equals("or"))
      { Expression nleft = negate(be.left); 
        Expression nright = negate(be.right); 
        Expression res = new BinaryExpression("&", nleft, nright); 
        res.setBrackets(true); 
        return res; 
      } 
      else if (op.equals("->exists"))
      { Expression nright = negate(be.right); 
        Expression res = new BinaryExpression("->forAll", be.left, nright); 
        res.setBrackets(true); 
        return res; 
      } 
      else if (op.equals("->forAll"))
      { Expression nright = negate(be.right); 
        Expression res = new BinaryExpression("->exists", be.left, nright); 
        res.setBrackets(true); 
        return res; 
      } 
      else if (op.equals("#"))
      { Expression nright = negate(be.right); 
        Expression res = new BinaryExpression("!", be.left, nright); 
        res.setBrackets(true); 
        return res; 
      } 
      else if (op.equals("!"))
      { Expression nright = negate(be.right); 
        Expression res = new BinaryExpression("#", be.left, nright); 
        res.setBrackets(true); 
        return res; 
      } 

      String nop = negateOp(be.operator); 
      if (!(nop.equals(be.operator))) 
      { return new BinaryExpression(nop,be.left,be.right); } 
      return new UnaryExpression("not",e); 
    } 
    else if (e instanceof UnaryExpression) 
    { UnaryExpression ue = (UnaryExpression) e; 
      if (ue.operator.equals("not"))
      { return ue.argument; } 
    } 
    return new UnaryExpression("not",e); 
  } // and other cases

  private static Expression simplifyIn(Expression le,
                                Expression re,
                                Vector vars)
  { if (re instanceof SetExpression)
    { SetExpression rse = (SetExpression) re;
      if (rse.isEmpty()) 
      { return new BasicExpression("false"); }
      if (rse.isSingleton())
      { Expression relem = rse.getElement(0);
        return simplify("=",le,relem,vars);
      }
    } // x : a \/ b  as  x : a or x : b etc?
    return new BinaryExpression(":",le,re);
  }

  private static Expression simplifyIn(Expression le,
                                Expression re)
  { if (re instanceof SetExpression)
    { SetExpression rse = (SetExpression) re;
      if (rse.isEmpty()) 
      { return new BasicExpression("false"); }
      if (rse.isSingleton())
      { Expression relem = rse.getElement(0);
        return simplify("=",le,relem,false);
      }
    } // x : a \/ b  as  x : a or x : b etc?
    return new BinaryExpression(":",le,re);
  }


  private static Expression simplifyIneq(String op, Expression left, 
                                         Expression right)
  { String lval = left.toString(); 
    String rval = right.toString(); 
    if (op.equals("<") || op.equals(">"))
    { if (lval.equals(rval)) 
      { return new BasicExpression("false"); } 
    }
    else if (op.equals("<=") || op.equals(">=") || op.equals("<:"))
    { if (lval.equals(rval))
      { return new BasicExpression("true"); } 
    } 
    try 
    { int x = Integer.parseInt(lval); 
      int y = Integer.parseInt(rval); 
      boolean val = VectorUtil.test(op,x,y);  
      return new BasicExpression("" + val); 
    } // and for long ints. 
    catch (Exception e) 
    { return new BinaryExpression(op,left,right); } 
  } 

  public boolean equals(Object obj) 
  { if (obj instanceof Expression && obj != null) 
    { return toString().equals(obj.toString()); } 
    else 
    { return false; } 
  }

  public abstract Expression filter(final Vector vars);

  public abstract Object clone(); // Not needed? 

  public void display() 
  { System.out.println(toString()); } 

  public Expression excel2Ocl(Entity target, Vector entities, Vector qvars, Vector antes)
  { return this; }  

  public Vector splitToCond0Cond1(Vector conds, Vector pars, Vector qvars1, Vector lvars1, Vector allvars)
  { return conds; }  // all formulae in antecedent should be binary

  public Vector splitToCond0Cond1Pred(Vector conds, Vector pars, 
                            Vector qvars1, Vector lvars1, Vector allvars, Vector allpreds)
  { return conds; }  // all formulae in antecedent should be binary

  public Vector splitToTCondPost(Vector tvars, Vector conds)
  { return conds; }  // all formulae in antecedent should be binary

  public Vector variableRanges(java.util.Map m, Vector pars)
  { return new Vector(); } 

  public Vector letDefinitions(java.util.Map m, Vector pars)
  { return new Vector(); } 

  static protected Expression filterEquality(final Expression l,
                                    final Expression r,
                                    final Expression e,
                                    final Vector vars)
  { boolean found = false;
    for (int i = 0; i < vars.size(); i++)
    { String var = (String) vars.elementAt(i);
      if (l.equalsString(var) ||  r.equalsString(var))
      { found = true;
        return e; 
      } 
    }
    return null;  
  }

  public boolean equalsString(final String s)
  { return s.equals(toString()); }

  abstract public Vector splitAnd(final Vector comps); 

  abstract public Vector splitOr(final Vector comps); 

  abstract public Expression expandMultiples(final Vector sms);

  abstract public Expression removeExpression(final Expression e);

  public Expression removeExpressions(final Vector el)
  { Expression res = (Expression) clone();
    for (int i = 0; i < el.size(); i++)
    { Expression e = (Expression) el.elementAt(i); 
      Expression res2 = res.removeExpression(e);
      if (res2 == null)
      { return null; }
      else 
      { res = res2; } 
    }
    return res; 
  }

 public static Expression removeTypePredicate(Expression cond, 
                                              String var, String typ)
 { if (cond instanceof BinaryExpression)
   { BinaryExpression bex = (BinaryExpression) cond; 
     if (bex.operator.equals(":") && var.equals(bex.left + "") && 
         typ.equals(bex.right + ""))
     { return new BasicExpression("true"); } 
   
     Expression c1 = removeTypePredicate(bex.left,var,typ); 
     Expression c2 = removeTypePredicate(bex.right,var,typ); 
     return simplify(bex.operator,c1,c2,false); 
   } 
   return cond; 
  } 

 
 public abstract boolean implies(final Expression e);

 public abstract boolean consistentWith(final Expression e);

 public abstract boolean selfConsistent(final Vector vars); 

 public boolean selfConsistent()
 { return true; }   // default 

 public abstract boolean subformulaOf(final Expression e);

 public abstract boolean conflictsWith(final Expression e); 

  public boolean conflictsWith(String op, Expression el,
                             Expression er)
  { return false; } 

public static boolean conflictsOp(String op1, String op2)
{ if (op1.equals("="))
  { return (op2.equals(">") || op2.equals("<") ||
            op2.equals("/=") || op2.equals("/<:"));
  }
  if (op1.equals("<"))
  { return (op2.equals(">") || op2.equals(">=") ||
            op2.equals("="));
  }
  if (op1.equals(">"))
  { return (op2.equals("<") || op2.equals("<=") ||
            op2.equals("="));
  }
  if (op1.equals("/="))
  { return op2.equals("="); }
  if (op1.equals("<:"))
  { return op2.equals("/<:"); }
  if (op1.equals(":"))
  { return op2.equals("/:"); }
  if (op1.equals("->includes"))
  { return op2.equals("->excludes"); }
  if (op1.equals("/<:"))
  { return (op2.equals("=") || op2.equals("<:")); }
  if (op1.equals("/:"))
  { return op2.equals(":"); }
  if (op1.equals("->excludes"))
  { return op2.equals("->includes"); }
  if (op1.equals("<="))
  { return op2.equals(">"); } 
  if (op1.equals(">="))
  { return op2.equals("<"); }
  return false;
}

/* I don't understand the purpose of this: */ 
public static boolean conflictsReverseOp(String op1, String op2)
{ if (op1.equals("="))
  { return (op2.equals(">") || op2.equals("<") ||
            op2.equals("/=") || op2.equals("/<:"));
  }
  if (op1.equals("<"))
  { return (op2.equals("<") || op2.equals("<=") ||
            op2.equals("="));
  }
  if (op1.equals(">"))
  { return (op2.equals(">") || op2.equals(">=") ||
            op2.equals("="));
  }
  if (op1.equals("/="))
  { return op2.equals("="); }
  if (op1.equals("/<:"))
  { return op2.equals("="); }
  if (op1.equals("<="))
  { return op2.equals("<"); } 
  if (op1.equals(">="))
  { return op2.equals(">"); }
  return false;
}




 public boolean conflictsWith(final Expression e, 
                              final Vector vars)
  { for (int i = 0; i < vars.size(); i++)
    { String var = (String) vars.elementAt(i);
      Expression myval = getSettingFor(var);
      Expression eval = e.getSettingFor(var);

      // System.out.println("1st exp sets " + var + " = " +
      //                   myval + " in " + toString()); 
      // System.out.println("2nd exp sets " + var + " = " + 
      //                   eval + " in " + e); 

      if (myval == null || eval == null)  
      {  }  /* ok */
      else if (myval.equals(eval))  { }
      else { return true; }  /* conflict */
    }
   return false; 
 }

  public static Expression evaluateExpAt(Expression e,
      Vector mapping, Vector allvars)
  { Expression e2 = (Expression) e.clone(); 
    BasicExpression be; 

    for (int i = 0; i < mapping.size(); i++)
    { Maplet setting = (Maplet) mapping.get(i);
      String var = (String) setting.source;
      Object val = setting.dest;
      if (val instanceof String) 
      { be = new BasicExpression((String) val); } 
      else 
      { be = (BasicExpression) val; } 
      Expression e1 = e2.substituteEq(var,be);
      e2 = e1.simplify(allvars);
    }
    return e2;
  }


 protected Expression getSettingFor(final String var)
 { Maplet mm = findSubexp(var);
   if (mm == null)  { return null; }
   else 
   { Expression ee = (Expression) mm.source;
     if (ee != null &&
         (ee instanceof BinaryExpression) && 
         ((BinaryExpression) ee).operator.equals("="))
    { BinaryExpression be = (BinaryExpression) ee;
      if (be.left.equalsString(var))
      { return  be.right; }
      else 
      { return be.left; } 
    }
    else 
    { return null; } 
   }
 }     

  abstract public Expression computeNegation4succ(final Vector actuators); 

  abstract public Vector computeNegation4ante(final Vector actuators);

  public Expression computeNegation4succ()
  { return null; }  // default  

  public Vector computeNegation4ante()
  { return new Vector(); }  // default

  public Expression satisfiedAt(State st,
                             Vector snames, 
                             String[] vals,
                             Vector cnames)
  { /* Tests if self satisfied at state 
       described by st and vals/snames */
    Expression temp;
    Expression res = (Expression) clone();

    for (int i = 0; i < snames.size(); i++)
    { String sen = (String) snames.get(i);
      String val = vals[i];
      temp = res.substituteEq(sen, 
                 new BasicExpression(val));
      res = (Expression) temp.clone(); 
    }

    Maplet mm = st.getProperties();
    if (mm != null && mm.source != null)
    { Vector subs = (Vector) mm.source;
      for (int j = 0; j < subs.size(); j++)
      { Maplet sub = (Maplet) subs.get(j);
        String act = (String) sub.source;
        Expression actval = 
              (Expression) sub.dest;
        temp =
              res.substituteEq(act,actval);
        res = (Expression) temp.clone();
      }
    }
    return res.simplify(cnames); 
  }

  public UpdateFeatureInfo updateFeature(String f)
  { return null; } // default

  public Expression updateExpression() { return null; } // default

  public Expression updateReference() { return null; }  // default

  /* What is this doing??: */ 
  public static Entity findTarget(Expression succ, String src)
  { if (succ instanceof BinaryExpression)
    { BinaryExpression be = (BinaryExpression) succ;
      Entity e1 = be.left.getEntity();
      Entity e2 = be.right.getEntity();
      if (e1 != null)
      { String e1name = e1.getName();
        if (e1name.equals(src)) { }
        else if (be.operator.equals("="))
        { return e1; }
      }
      if (e2 != null)
      { String e2name = e2.getName();
        if (e2name.equals(src)) { }
        else
        { return e2; }
      }
      return null;
    }
    return succ.getEntity();
  } // but could be more implicit: x : objs.att

  // Is this used? What does it do? 
  public static Entity getTargetEntity(Expression e)
  { if (e instanceof BinaryExpression)
    { BinaryExpression be = (BinaryExpression) e; 
      Expression l = be.left; 
      Expression r = be.right; 
      Entity e1 = l.getEntity(); 
      Entity e2 = r.getEntity(); 
      if (be.operator.equals("=")) 
      { if (e1 == null) 
        { return e2; } 
        return e1; 
      } 
      if (be.operator.equals(":"))  // and /:, <:, /<:, ->includes, ->includesAll  ??
      { if (e2 == null)
        { return e1; } 
        return e2; 
      } 
    } 
    return null; 
  } 

  public static String objectType(Expression typ, java.util.Map env, boolean local)
  { if (typ != null)
    { if (typ.umlkind == CLASSID)
      { return typ.entity.objectType(); }  
      return "" + typ.binvariantForm(env,local);  
    } 
    return typ + ""; 
  }

  // Strange operation: 
  public static Vector ancestorAddStatements(Expression typ, BExpression var)
  { Vector res = new Vector(); 
    if (typ != null)
    { if (typ.umlkind == CLASSID)
      { if (typ.entity != null && typ.entity.hasSuperclass())
        { Entity sup = typ.entity.getSuperclass(); 
          return sup.ancestorAddStatements(var);
        }
      }  
      return res;  
    } 
    return res; 
  }

  public static Vector commonConjSubformula(Expression l,
                                            Expression r)
  { Vector res = new Vector(); // comm, lrem, rrem
    if (l.equals(r)) // also l equals r.reverse() for =
    { res.add(l);
      res.add(null);
      res.add(null);
      return res;
    }
    if (l instanceof BinaryExpression)
    { BinaryExpression bel = (BinaryExpression) l;
      if (bel.operator.equals("&"))
      { if (bel.left.equals(r))
        { res.add(bel.left);
          res.add(bel.right);
          res.add(null);
          return res;
        }
        if (bel.right.equals(r))
        { res.add(bel.right);
          res.add(bel.left);
          res.add(null);
          return res;
        }
        Vector res1 = commonConjSubformula(bel.left,r);
        Vector res2 = commonConjSubformula(bel.right,r);
        res = tripleConj(res1,res2);
        if (res.get(0) != null)
        { return res; }
      }
    }
    if (r instanceof BinaryExpression)
    { BinaryExpression ber = (BinaryExpression) r;
      if (ber.operator.equals("&"))
      { if (ber.left.equals(l))
        { res.add(ber.left);
          res.add(null);
          res.add(ber.right);
          return res;
        }
        if (ber.right.equals(l))
        { res.add(ber.right);
          res.add(null);
          res.add(ber.left);
          return res;
        }
        Vector res1 = commonConjSubformula(l,ber.left);
        Vector res2 = commonConjSubformula(l,ber.right);
        res = tripleConj(res1,res2);
        if (res.get(0) != null)
        { return res; }
      }
    }
    res.add(null);
    res.add(l);
    res.add(r);
    return res;
  }
          
  private static Vector tripleConj(Vector v1, Vector v2)
  { System.out.println("Called with [[" + v1 + "]]  [[" + v2); 

    Vector res = new Vector();
    int n = v1.size();
    Expression comm1 = (Expression) v1.get(0);
    Expression comm2 = (Expression) v2.get(0);
    ((Expression) v2.get(1)).removeExpression(comm1);
    ((Expression) v2.get(2)).removeExpression(comm1);
    ((Expression) v1.get(1)).removeExpression(comm2);
    ((Expression) v1.get(2)).removeExpression(comm2);
    for (int i = 0; i < n; i++)
    { Expression e1 = (Expression) v1.get(i);
      if (i < v2.size())
      { Expression e2 = (Expression) v2.get(i);
        res.add(simplify("&",e1,e2,new Vector()));
      } 
      else 
      { res.add(e1); } // hack
    }
    return res;
  }

  public Expression matchCondition(String op, String f,
                     Expression exbe)
  { // default:
    return new BasicExpression("false"); 
  }

  public static Attribute hasPrimaryKey(Expression right)
  { Type tr = right.getElementType(); 
    if (tr.entity != null) 
    { Entity e = tr.entity; 
      Vector keys = e.allDefinedUniqueAttributes(); 
        // search for equation lft.key = value in right
      if (keys.size() > 0)
      { return (Attribute) keys.get(0); } 
    } 
    return null; 
  } 

  public abstract Expression featureSetting(String var, String feat, Vector r); 

  public Expression featureSetting2(String var, String feat, Vector r)
  { r.add(this); 
    return null; 
  }  // default

  public Expression featureAdding2(String var, Vector r)
  { r.add(this); 
    return null; 
  }  // default

  public static boolean nakedUses(String f, Vector uses)
  { boolean res = true; 
    for (int i = 0; i < uses.size(); i++) 
    { Expression use = (Expression) uses.get(i); 
      if (use instanceof BasicExpression) 
      { BasicExpression be = (BasicExpression) use; 
        if (be.nakedUse(f)) { } 
        else 
        { return false; } 
      } 
      else 
      { return false; } 
    } 
    return res; 
  }  

  public abstract int syntacticComplexity(); 

  public abstract int cyclomaticComplexity(); 

  public Expression isExistsForall(Vector foralls, Expression tracest)
  { return null; }

  public Expression isForall(Vector foralls, Expression tracest)
  { return null; }

  public abstract void changedEntityName(String oldN, String newN); 
} 

