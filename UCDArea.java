/******************************
* Copyright (c) 2003,2019 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/

/*
 * Classname : UCDArea
 * 
 * Version information : 1.9
 *
 * Date :  June 2019
 * 
 * Description: This class describes the area that all the painting for 
 * the CD diagram will be performed and deals with painting them
 * depending on the users actions with the mouse. (Detecting events)

   package: Class diagram GUI

 */


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.io.*;
import java.awt.print.*; 

import javax.swing.border.Border;
import java.util.EventObject;
import java.util.Vector;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer; 


public class UCDArea extends JPanel 
    implements MouseListener, MouseMotionListener, KeyListener, Printable
{ // these indicate which option user has chosen 
    public static final int SLINES = 0;
    public static final int POINTS = 1;
    public static final int EVENTS = 2;
    public static final int OVAL = 3;
    public static final int DLINES = 4;
    public static final int EDIT = 5; 
    public static final int ACLINES = 6;
 
    public static final int SOLID = 0;
    public static final int DASHED = 1;
    
    final static BasicStroke stroke = 
                               new BasicStroke(2.0f);

    public static final int INERT = -1; 
    public static final int SENSOR = 0; 
    public static final int ACTUATOR = 1; 
    public static final int CONTROLLER = 2; 

    public static final int NODECOMP = 0; 
    public static final int HORIZ = 1; 
    public static final int PHASE = 2; 
    public static final int HIERARCH = 3; 

    private int decomposition = NODECOMP; 

    public static final int DCFD = 0; 
    public static final int CLASSDIAGRAM = 1; 

    private int view = DCFD;  // view, class or DCFD. 

    // Get screen size for size of the scroll bar view
    Dimension screenSize =
      Toolkit.getDefaultToolkit().getScreenSize();   
    Dimension preferredSize = screenSize; 
  
    // default mode - no action 
    private int mode = INERT;
     
    private int componentMode = SENSOR; 
    private String componentName; 

    static final int NOEDIT = 0; 
    static final int DELETING = 1; 
    static final int MOVING = 2;
    static final int MODIFY = 3; 
    private int editMode = NOEDIT; 

    private VisualData selectedVisual;
    private ModelElement selectedComponent;
    
    private int x1, y1;  // x1,y1 store where mouse 
                         // has been pressed
    private int x2, y2;  // end of line coordinates
    private int prevx, prevy;	  // holds value when
                         // dragged off first point
    private boolean firstpress = false;   
                         // True when user drawing a
                         // line
    
    // True if the user pressed, dragged or released 
    // the mouse outside of
    // the rectangle; false otherwise:
    private boolean pressOut = false;   

    // Dialog definitions
    // private EvtNameDialog nameDialog;
    
    private Vector visuals = new Vector();  // of VisualData
      // holds all the visuals drawn
    private Vector waypoints = new Vector(); 

    private Vector eventlist = new Vector(); 	
      // holds all the events entered	 
    private Vector componentNames = new Vector(); // of String
      // names of all components
    private Vector sensors = new Vector();    
      // all sensor statemachines
    private Vector actuators = new Vector();   
      // all actuator statemachines
    private Vector processes = new Vector();   
      // all controller, general use case visuals
    private Vector types = new Vector(); // of Type 
    private Vector entities = new Vector(); // of Entity
    private Vector associations = new Vector(); // of Association
    private Vector invariants = new Vector(); // SafetyInvariant
    private Vector constraints = new Vector(); // of Constraint
    private Vector generalisations = new Vector(); // of Generalisation
    private Vector useCases = new Vector(); // of OperationDescription or UseCase
    private Vector activities = new Vector(); // of Behaviour

    private BComponent bcontroller = null;  // The B Controller
    private Vector families = new Vector(); // of InheritanceFamily
    // Counts of number of visual items drawn
    private static int rectcount = 0;
    private static int linecount = 0;
    private static int ovalcount = 0;
    private static String systemName = ""; 
    private Vector importList = new Vector(); // of String

    private Vector imported = new Vector(); // of String
    private Vector entitymaps = new Vector(); // of EntityMatching

  // Parent frame: 
    UmlTool parent; 
  // Dialog: 
  ExDialog2 dialog; 
  AttEditDialog attDialog; 
  AstEditDialog astDialog;
  OperationEditDialog opDialog; 
  ActivityEditDialog actDialog; 
  ModifyEntityDialog modifyDialog;
  // ModifyTypeDialog modifyTypeDialog;  
  UseCaseDialog ucDialog; 
  BacktrackDialog backtrackDialog; 
  EntityCreateDialog entDialog; 
  ModifyUseCaseDialog editucDialog; 
  WinHandling state_win = new WinHandling(); 


  public UCDArea(UmlTool par)
  { setBackground(Color.white);
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);  	
    parent = par; 
  }
    
  public void setVisuals(Vector v) 
  { visuals = v; } 

  public void setComponents(Vector c) 
  { componentNames = c; } 

  public void setActuators(Vector a) 
  { actuators = a; } 

  public Vector getActuators() 
  { return actuators; } 

  public String getSystemName()
  { return systemName; } // The name of the package containing the syste

  public void setSystemName(String n)
  { systemName = n; } // The name of the package containing the system

  public void setSensors(Vector s) 
  { sensors = s; } 

  public Vector getSensors() 
  { return sensors; } 

  public Vector getTypes()
  { return types; } 

  public Vector getAllComponents()
  { Vector res = sensors; 
    res.addAll(actuators); 
    return res; 
  } 

  public Vector getConstraints()
  { return constraints; } 

  public Vector getAssociations()
  { return associations; } 

  public Vector getAssociationClasses()
  { Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      if (e.isAssociationClass())
      { res.add(e); } 
    } 
    return res; 
  }  

  public Vector getGeneralUseCases() 
  { Vector res = new Vector(); 
    for (int i = 0; i < useCases.size(); i++) 
    { Object uc = useCases.get(i); 
      if (uc instanceof UseCase)
      { res.add(uc); } 
    } 
    return res; 
  } 

  public void addUseCases(Vector ucs)
  { for (int i = 0; i < ucs.size(); i++) 
    { UseCase uc = (UseCase) ucs.get(i);
      String nme = uc.getName();  
      UseCase uc0 = (UseCase) ModelElement.lookupByName(nme,useCases); 
      if (uc0 != null) 
      { System.out.println("Existing use case with name: " + nme); 
        useCases.remove(uc0); 
      }
    }
    for (int i = 0; i < ucs.size(); i++) 
    { UseCase uc = (UseCase) ucs.get(i);
      addGeneralUseCase(uc); 
    } 
  } 

 
  public Vector getEntities()
  { return entities; } 

  public Vector getSourceEntities()
  { Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity et = (Entity) entities.get(i); 
      if (et.isSourceEntity())
      { res.add(et); } 
    } 
    return res; 
  } 

  public Vector getTargetEntities()
  { Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity et = (Entity) entities.get(i); 
      if (et.isTargetEntity())
      { res.add(et); } 
    } 
    return res; 
  } 

  public Vector getInterfaces()
  { Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i);
      if (e.isInterface())
      { res.add(e); } 
    } 
    return res; 
  } 

  public void addImport(String imprt) 
  { if (importList.contains(imprt)) { } 
    else 
    { importList.add(imprt); } 
  } 


  public void typeCheck()
  { for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      e.typeCheckOps(types,entities); 
    } 

    for (int j = 0; j < useCases.size(); j++) 
    { if (useCases.get(j) instanceof UseCase)
      { UseCase uc = (UseCase) useCases.get(j); 
        uc.typeCheck(types,entities); 
      } 
    } 
  } // and entity activities. 


  public void deleteUseCase(String nme)
  { if (nme == null)
    { return; } 
    UseCase uc = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (uc == null) { return; } 
    useCases.remove(uc); 
    removeVisual(nme); 
  } 

  public void deleteUseCase(UseCase uc) 
  { useCases.remove(uc); }  // and the visual? 

  public void interactiveEditUseCase(String nme) 
  { if (nme == null)
    { return; } 
    UseCase uc = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (uc == null) { return; } 

    UseCaseEditor ucedt = new UseCaseEditor(this,uc,entities); 
    // ucedt.pack();
    // ucedt.setVisible(true);
    // Vector invs = ucedt.getInvs(); 
    // for (int i = 0; i < invs.size(); i++) 
    // { SafetyInvariant inv = (SafetyInvariant) invs.get(i); 
    //   if (inv != null)
    //   { addUseCasePostcondition(uc,inv); }
    // } 
  } 

  public void interactiveEditKM3() 
  { 
    KM3Editor km3edt = new KM3Editor(this,entities,useCases); 
    // ucedt.pack();
    // ucedt.setVisible(true);
    // Vector invs = ucedt.getInvs(); 
    // for (int i = 0; i < invs.size(); i++) 
    // { SafetyInvariant inv = (SafetyInvariant) invs.get(i); 
    //   if (inv != null)
    //   { addUseCasePostcondition(uc,inv); }
    // } 
  } 

  public void editUseCase(String nme) 
  { if (nme == null)
    { return; } 
    UseCase uc = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (uc == null) { return; } 

    if (editucDialog == null)
    { editucDialog = new ModifyUseCaseDialog(parent); 
      editucDialog.pack();
      editucDialog.setLocationRelativeTo(this);
    }; 
    editucDialog.setVisible(true); 

    if (editucDialog.isAddPre())
    { Invariant inv = parent.createStaticInvariant("","","");
      if (inv != null)
      { addUseCasePrecondition(uc,inv); }
    } 
    else if (editucDialog.isAddPost())
    { Invariant inv = parent.createStaticInvariant("","","");
      if (inv != null)
      { addUseCasePostcondition(uc,inv); }
    }     
    else if (editucDialog.isEditConstraint())
    { Vector res = displayUseCaseInvariants(uc);
      editUseCaseConstraints(uc,res); 
      // edit the constraint and replace in same position in uc
    } 
    else if (editucDialog.isRemovePost())
    { Vector res = displayUseCaseInvariants(uc); 
      removeUseCaseConstraints(uc,res); 
    } 
    else if (editucDialog.isRemovePre())
    { Vector res = displayUseCaseInvariants(uc); 
      removeUseCaseConstraints(uc,res); 
    } 
    else if (editucDialog.isAddExtends())
    { addUseCaseExtends(uc); }    
    else if (editucDialog.isAddIncludes())
    { addUseCaseIncludes(uc); }    
    else if (editucDialog.isInheritFrom())
    { setUseCaseExecutionMode(uc); } // Execution mode   
    else if (editucDialog.isExpand())
    { expandUseCase(uc); } 
    else if (editucDialog.isGenInverse())
    { UseCase invert = uc.invert(types,entities); 
      System.out.println("Inverted use case: " + invert.display());
 
      UseCase ucrev = 
        (UseCase) ModelElement.lookupByName(invert.getName(),useCases); 
      if (ucrev != null) 
      { useCases.remove(ucrev); }  // and delete any visual for it.  
      addGeneralUseCase(invert);   
    } 
    else if (editucDialog.isAddInv())
    { Invariant inv = parent.createStaticInvariant("","","");
      if (inv != null)
      { addUseCaseInvariant(uc,inv); }
    } 
    else if (editucDialog.isAddAtt())
    { if (attDialog == null)
      { attDialog = new AttEditDialog(parent);
        attDialog.pack();
        attDialog.setLocationRelativeTo(this);
      }
      attDialog.setOldFields("","",ModelElement.INTERNAL,"",false,false,true);
      attDialog.setVisible(true);
   
      String attnme = attDialog.getName(); 
      if (attnme == null) { return; } 
      boolean alreadyDefined = uc.hasAttribute(attnme); 
      if (alreadyDefined)
      { System.err.println("Use case already has attribute " + attnme + " not added"); 
        return; 
      }

      String typ = attDialog.getAttributeType();

      if (typ == null) { return; } 
      Type tt = Type.getTypeFor(typ,types,entities); 
      if (tt == null) 
      { if ("int".equals(typ) || "long".equals(typ) || typ.equals("String") || 
            typ.equals("double") || typ.equals("boolean"))
        { // System.out.println("Inbuilt type: valid");
          tt = new Type(typ,null);
        }
        else
        { System.out.println("Invalid type name: " + typ);
          JOptionPane.showMessageDialog(null, "Error: invalid type " + typ, 
                                      "", JOptionPane.ERROR_MESSAGE);  
 
          tt = null;
          return; 
        }
      } 

      int kind = attDialog.getKind(); 
      Attribute att = new Attribute(attnme,tt,kind);
      att.setElementType(tt.getElementType()); 

      Vector contexts = new Vector(); 
      // contexts.add(ent); 

      String ini = attDialog.getInit(); 
      if (ini != null && !ini.equals(""))
      { Compiler2 comp = new Compiler2();
        System.out.println("Initialisation: " + ini);
        comp.nospacelexicalanalysis(ini); 
        Expression initExp = comp.parse();
        if (initExp == null) 
        { System.err.println("!!! Invalid initialisation expression: " + ini); } 
        else  
        { boolean b = initExp.typeCheck(types,entities,contexts,new Vector()); 
          Expression ie2 = initExp.simplify(); 
          String iqf = ie2.queryForm(new java.util.HashMap(),true);
          // System.out.println("Initialisation: " + iqf);
          att.setInitialValue(iqf); 
          att.setInitialExpression(ie2); 
        }
      }
      att.setFrozen(attDialog.getFrozen()); 
      att.setUnique(attDialog.getUnique()); 
      if (attDialog.getUnique() == true)
      { System.out.println(">>> Primary key attribute. Must be of type String"); } 
      // not supported for use cases. 

      att.setInstanceScope(attDialog.getInstanceScope()); 
      // must be static. 

      uc.addAttribute(att);
    }
    else if (editucDialog.isAddOp())
    { addUseCaseOperation(uc); } 
    else if (editucDialog.isRemoveAtt())
    { removeAttributeFromUseCase(uc); } 
    else if (editucDialog.isRemoveOp())
    { removeOperationFromUseCase(uc); } 
    else if (editucDialog.isEditOp())
    { editUseCaseOperation(uc); } 
  } 

  public void addUseCasePrecondition(UseCase uc, Invariant inv)
  { // System.out.println("Select entity that invariant is attached to");
    if (inv == null) { return; } 
    String anames = inv.ownerText; 

    //  JOptionPane.showInputDialog("Enter entity name:");
    if (anames == null) 
    { return; } 
    StringTokenizer st = 
          new StringTokenizer(anames); 
    Entity owner = null; 
    
    while (st.hasMoreTokens())
    { String se = st.nextToken().trim();
      
      Entity e = (Entity) ModelElement.lookupByName(se,entities); 
      if (e != null)
      { owner = e; 
        break; 
      }  
    } 

    Vector contexts = new Vector(); 
    if (owner != null) 
    { contexts.add(owner); }  
    if (uc.classifier != null) 
    { contexts.add(uc.classifier); } 

    Vector pars = new Vector(); 
    if (uc != null) 
    { pars.addAll(uc.getParameters()); 
      pars.addAll(uc.getOwnedAttribute()); 
    } 

    boolean tc = inv.typeCheck(types,entities,contexts,pars); 
    if (tc) 
    { System.out.println(">>> Precondition type-checked correctly"); } 
    else 
    { System.out.println("!!! Precondition not correctly typed!"); } 

     
    Constraint cons = new Constraint((SafetyInvariant) inv,new Vector()); 
    cons.setOwner(owner); 
    cons.typeCheck(types,entities,contexts,pars); // to identify variables
    Vector lvars = new Vector(); 
    Vector qvars = cons.secondaryVariables(lvars,pars);  
    uc.addPrecondition(cons); 
    cons.setUseCase(uc); 
  } 


  public void addUseCaseInvariant(UseCase uc, Invariant inv)
  { // System.out.println("Select entity that invariant is scoped by");
    if (inv == null) { return; } 
    String anames = inv.ownerText; 
    //  JOptionPane.showInputDialog("Enter entity name:");
    if (anames == null) 
    { return; } 
    StringTokenizer st = 
          new StringTokenizer(anames); 
    Entity owner = null; 
    int atindex = -1; 
    String preEntity = ""; 
    
    while (st.hasMoreTokens())
    { String se = st.nextToken().trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        se = se.substring(0,atindex); 
      } 
      
      Entity e = (Entity) ModelElement.lookupByName(se,entities); 
      if (e != null)
      { owner = e; 
        break; 
      }  
    } 
    // also could refer to pre-entity

    Vector contexts = new Vector(); 
    if (owner != null) 
    { contexts.add(owner); }  
    if (uc != null && uc.classifier != null) 
    { contexts.add(uc.classifier); } 

    Vector pars = new Vector(); 
    if (uc != null) 
    { pars.addAll(uc.getParameters()); 
      pars.addAll(uc.getOwnedAttribute()); 
    } 

    boolean tc = inv.typeCheck(types,entities,contexts,pars); 
    if (tc) 
    { System.out.println("Invariant type-checked correctly"); } 
    else 
    { System.out.println("Invariant not correctly typed!"); } 

    Constraint cons = new Constraint((SafetyInvariant) inv,new Vector()); 
    cons.setOwner(owner); 
    if (preEntity.length() > 0)
    { cons.setisPre(true); } 

    cons.typeCheck(types,entities,contexts,pars); // to identify variables
    Vector lvars = new Vector(); 
    Vector qvars = cons.secondaryVariables(lvars,pars);  
    uc.addInvariant(cons); 
    cons.setUseCase(uc); 
  } 


  public void addUseCasePostcondition(UseCase uc, Invariant inv)
  { // System.out.println("Enter entity (the primary quantifier range)");
    // String anames = 
    //   JOptionPane.showInputDialog("Enter entity name (or leave blank):");
    if (inv == null) { return; } 
    String anames = inv.ownerText; 
    if (anames == null) 
    { return; } 
    StringTokenizer st = 
          new StringTokenizer(anames); 
    Entity owner = null; 
    int constraintType = 1; 
    int atindex = -1; 
    String preEntity = ""; 
    Statement stat; 
    Type resultType = uc.getResultType(); 
    
    while (st.hasMoreTokens())
    { String se = st.nextToken().trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        se = se.substring(0,atindex); 
      } 
      
      Entity e = (Entity) ModelElement.lookupByName(se,entities); 
      if (e != null)
      { owner = e; 
        break; 
      }  
    } 

    Vector contexts = new Vector(); 
    if (owner != null) 
    { contexts.add(owner); }  
    if (uc.classifier != null) 
    { contexts.add(uc.classifier); } 

    Vector ucparams = uc.getParameters(); 
    Vector newparams = new Vector(); 
    newparams.addAll(ucparams); 
    
    if (resultType != null) 
    { newparams.add(new Attribute("result",resultType,ModelElement.INTERNAL)); }
    Vector ucatts = uc.getOwnedAttribute(); 
    newparams.addAll(ucatts); 

    boolean tc = inv.typeCheck(types,entities,contexts,newparams); 
    if (tc) 
    { System.out.println("Postcondition type-checked correctly"); } 
    else 
    { System.out.println("Postcondition not correctly typed!"); } 

    Constraint cons = new Constraint((SafetyInvariant) inv,new Vector()); 
    cons.setOwner(owner); 
    if (preEntity.length() > 0) 
    { cons.setisPre(true); 
      System.out.println("Owner in pre-state form"); 
    } 
    cons.typeCheck(types,entities,contexts,newparams); // to identify variables


   
    if (uc != null) 
    { uc.addPostcondition(cons); 
      cons.setUseCase(uc); 
      uc.resetDesign(); 
      System.out.println("The use case design has been reset"); 
    }       
  } 

  public void resetDesigns()
  { for (int i = 0; i < useCases.size(); i++) 
    { if (useCases.get(i) instanceof UseCase)
      { UseCase uc = (UseCase) useCases.get(i); 
        uc.resetDesign(); 
        System.out.println("The use case design of " + uc.getName() + " has been reset");
      } 
    }
    BSystemTypes.resetDesigns();  
    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      ent.removeDerivedOperations(); 
    } 
  }  
      
  public void removeUseCaseConstraints(UseCase uc, Vector cons)
  { // for (int i = 0; i < cons.size(); i++) 
    // { uc.removeConstraint((Constraint) cons.get(i)); } 
    uc.removeConstraints(cons); // also resets the design
    System.out.println("Warning: the design is no longer valid and needs to be recreated"); 
    // re-analyse the use case
  } 

  public void editUseCaseConstraints(UseCase uc, Vector cons)
  { if (cons.size() > 0) 
    { Constraint cc = (Constraint) cons.get(0); 
      SInvEditDialog sinvDialog = new SInvEditDialog(parent); 
      sinvDialog.pack();
      sinvDialog.setLocationRelativeTo(this);
      sinvDialog.setOldFields(cc.getOwner() + "", "" + cc.antecedent(),
                              "" + cc.succedent(),true,false,cc.isOrdered());
      sinvDialog.setVisible(true);
      String anames = sinvDialog.getEntity(); 
      if (anames == null) 
      { return; } 
      StringTokenizer st = 
          new StringTokenizer(anames); 
      Entity owner = null; 
      int atindex = -1; 
      String preEntity = ""; 
    
      while (st.hasMoreTokens())
      { String se = st.nextToken().trim();
        atindex = se.indexOf('@'); 
        if (atindex > 0)
        { preEntity = se; 
          se = se.substring(0,atindex); 
        } 
      
        Entity e = (Entity) ModelElement.lookupByName(se,entities); 
        if (e != null)
        { owner = e; 
          break; 
        }  
      } 
  
      String sAssump = sinvDialog.getAssumption();
      String sConc = sinvDialog.getConclusion();
      if (sAssump != null && sConc != null)
      { System.out.println("Updating invariant");
        Compiler2 comp = new Compiler2(); 
        comp.nospacelexicalanalysis(sAssump);
        Vector antesymbs = new Vector(); 
        antesymbs.add("|"); antesymbs.add("="); antesymbs.add(":"); 
        Vector messages = new Vector(); 
        comp.checkSyntax(owner,entities,antesymbs,messages); 
        if (messages.size() > 0)
        { System.err.println(messages); } 
        Expression eAssump = comp.parse();
        if (eAssump == null)
        { eAssump = new BasicExpression("true"); }
        comp = new Compiler2(); 
        comp.nospacelexicalanalysis(sConc);
        Vector succsymbs = new Vector(); 
        succsymbs.add("|");  
        comp.checkSyntax(owner,entities,succsymbs,messages); 
        if (messages.size() > 0)
        { System.err.println(messages); } 
        Expression eConc = comp.parse();
        if (eConc == null)
        { eConc = new BasicExpression("true"); }

        // boolean isSys = sinvDialog.isSystem();
        // boolean isCrit = sinvDialog.isCritical();
        // boolean isBehav = sinvDialog.isBehaviour(); 
        boolean isOrd = sinvDialog.isOrdered(); 

        Type resultType = uc.getResultType(); 

        SafetyInvariant i2 = new SafetyInvariant(eAssump,eConc);

        // String oldowner = ""; 
        // if (cc.getOwner() != null) 
        // { oldowner = cc.getOwner().getName(); } 

        // String anames = 
        //  JOptionPane.showInputDialog("Enter entity name (or leave blank):", oldowner);
       
       Vector contexts = new Vector(); 
       if (owner != null) 
       { contexts.add(owner); }  
       // contexts.add(uc.classifier); ?? 

        Vector ucparams = uc.getParameters(); 
        Vector newparams = new Vector(); 
        newparams.addAll(ucparams); 
        if (resultType != null) 
        { newparams.add(new Attribute("result",resultType,ModelElement.INTERNAL)); }
        Vector ucatts = uc.getOwnedAttribute(); 
        newparams.addAll(ucatts); 

          // i2.setSystem(isSys);
          // i2.setCritical(isCrit);
          // i2.setBehavioural(isBehav); 
        i2.setOrdered(isOrd);           
        if (isOrd)
        { String ordBy = JOptionPane.showInputDialog("Enter ordering expression:");
          comp = new Compiler2(); 
          comp.nospacelexicalanalysis(ordBy); 
          Expression ordByExp = comp.parse(); 
          i2.setOrderedBy(ordByExp); 
        } 

        i2.typeCheck(types,entities,contexts,newparams); 
        cc.update(i2,new Vector()); 
        cc.setOwner(owner); 
        cc.typeCheck(types,entities,contexts,newparams); // to identify variables
        System.out.println("Constraint modified: " + cc);       
        resetDesigns(); 
        System.out.println("Warning: the design is no longer valid and needs to be recreated"); 
      }
      else
      { System.out.println("Invalid syntax -- not added"); }

      // re-analyse the use case
    } 
  } 

  public void generateDesign()
  { for (int i = 0; i < useCases.size(); i++) 
    { Object obj = useCases.get(i); 
      if (obj instanceof UseCase) 
      { UseCase uc = (UseCase) obj; 
        uc.resetDesign();
      } 
    } 
    BSystemTypes.resetDesigns();   // also clears collects, etc for entity ops. 
 
    for (int i = 0; i < useCases.size(); i++) 
    { Object obj = useCases.get(i); 
      if (obj instanceof UseCase) 
      { UseCase uc = (UseCase) obj; 
        useCaseToDesign(uc);
      }  
    } 
  } 

  public void showUCDependencies() 
  { for (int i = 0; i < useCases.size(); i++) 
    { Object ob = useCases.get(i); 
      if (ob instanceof UseCase) 
      { UseCase uc = (UseCase) ob; 
        Vector wrents = new Vector(); 
        Vector rdents = new Vector(); 
        uc.classDependencies(entities,associations,rdents,wrents);
        for (int j = 0; j < rdents.size(); j++) 
        { Entity e = (Entity) rdents.get(j); 
          if (wrents.contains(e))
          { drawDependency(uc,e,"<<writes>>"); 
            drawDependency(e,uc,"<<reads>>"); 
          } 
          else 
          { drawDependency(e,uc,"<<reads>>"); } 
        } 
        wrents.removeAll(rdents); 
        for (int j = 0; j < wrents.size(); j++) 
        { Entity f = (Entity) wrents.get(j); 
          drawDependency(uc,f,"<<writes>>"); 
        } 
      }
    }  
  } 

  private void useCaseToDesign(UseCase uc)
  { if (uc.isDerived()) 
    { return; } 
  
    uc.analyseConstraints(types, entities, associations); 
    uc.analyseDependencies(associations); 
    uc.mapToDesign(types,entities,associations); 
  } 

  private void addUseCaseExtends(UseCase uc)
  { String nme = 
      JOptionPane.showInputDialog("Enter extension use case name:");
    UseCase ucext = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (ucext == null) 
    { System.err.println("Invalid use case name: " + nme);  
      return; 
    } 

    // check that ucinc is not already an extension of an extends, or inclusion of 
    // an includes: 
    for (int i = 0; i < useCases.size(); i++) 
    { UseCase uc1 = (UseCase) useCases.get(i); 
      if (uc1.hasExtension(ucext))
      { System.err.println("Cannot have " + nme + " as extension of two usecases!"); 
        return; 
      } 
    } 

    for (int i = 0; i < useCases.size(); i++) 
    { UseCase uc1 = (UseCase) useCases.get(i); 
      if (uc1.hasInclusion(ucext))
      { System.err.println("Cannot have " + nme + " as extension and inclusion!"); 
        return; 
      } 
    } 

    Extend ee = new Extend(uc,ucext); 
    uc.addExtension(ee);  
    // Draw dashed line from ucext to uc
    drawDependency(ucext, uc, "<<extend>>"); 
  }

  private void setUseCaseExecutionMode(UseCase uc)
  { String nme = 
      JOptionPane.showInputDialog("Execution mode? (incremental/backtracking/bx/none):");
    if (nme.startsWith("i"))
    { uc.setIncremental(true); } 
    else if (nme.startsWith("n"))
    { uc.setIncremental(false); }
    else if (nme.startsWith("bx"))
    { uc.setBx(true); }  
    else if (nme.startsWith("ba"))
    { createBacktrackingSpecification(uc); } 
  }

  private void addUseCaseIncludes(UseCase uc)
  { String nme = 
      JOptionPane.showInputDialog("Enter inclusion use case name:");
    UseCase ucinc = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (ucinc == null) 
    { System.err.println("Invalid use case name: " + nme); 
      JOptionPane.showMessageDialog(null, "Error: no use case " + nme, 
                                      "",JOptionPane.ERROR_MESSAGE);  
      return; 
    } 

    // check that ucinc is not already an extension of an extends: 
    for (int i = 0; i < useCases.size(); i++) 
    { UseCase uc1 = (UseCase) useCases.get(i); 
      if (uc1.hasExtension(ucinc))
      { System.err.println("Cannot have " + nme + " as extension and inclusion!"); 
        JOptionPane.showMessageDialog(null, "Error: " + nme + " is extension & inclusion!", 
                                      "",JOptionPane.ERROR_MESSAGE);  
        return; 
      } 
    } 

    Include ee = new Include(uc,ucinc); 
    uc.addInclude(ee);  
    drawDependency(uc, ucinc, "<<include>>"); 
  }

  private void removeUseCaseIncludes(UseCase uc)
  { String nme = 
      JOptionPane.showInputDialog("Enter inclusion use case name:");
    UseCase ucinc = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (ucinc == null) 
    { System.err.println("Invalid use case name: " + nme); 
      JOptionPane.showMessageDialog(null, "Error: no use case " + nme, 
                                      "",JOptionPane.ERROR_MESSAGE);  
      return; 
    } 

    // check that ucinc is not already an extension of an extends: 
    uc.removeInclude(nme);  
    removeDependency(uc, ucinc, "<<include>>"); 
  }

  private void removeUseCaseExtends(UseCase uc)
  { String nme = 
      JOptionPane.showInputDialog("Enter extension use case name:");
    UseCase ucinc = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (ucinc == null) 
    { System.err.println("Invalid use case name: " + nme); 
      JOptionPane.showMessageDialog(null, "Error: no use case " + nme, 
                                      "",JOptionPane.ERROR_MESSAGE);  
      return; 
    } 

    // check that ucinc is not already an extension of an extends: 
    uc.removeExtend(nme);  
    removeDependency(uc, ucinc, "<<extend>>"); 
  }

  private void addUseCaseSuperclass(UseCase uc)
  { String nme = 
      JOptionPane.showInputDialog("Enter superclass use case name:");
    UseCase ucext = (UseCase) ModelElement.lookupByName(nme,useCases); 

    if (ucext != null) 
    { uc.setSuperclass(ucext); } 
  }  // Does this actually do anything? 

  private void addUseCaseOperation(UseCase uc)
  { if (opDialog == null)
    { opDialog = new OperationEditDialog(parent);
      opDialog.pack();
      opDialog.setLocationRelativeTo(this);
    }
    opDialog.setOldFields("","","","","",true);
    opDialog.setStereotypes(null); 
    opDialog.setVisible(true);
   
    String nme = opDialog.getName(); 
    String typ = opDialog.getType(); 
    boolean query = opDialog.getQuery(); 

    if (nme == null)
    { System.err.println("No name specified"); 
      JOptionPane.showMessageDialog(null, "Error: no name!", 
                                      "",JOptionPane.ERROR_MESSAGE);  
      return; 
    } 

    if (typ == null && query) 
    { System.err.println("Error: query operation without type"); 
      JOptionPane.showMessageDialog(null, "Error: no return type!", 
                                      "",JOptionPane.ERROR_MESSAGE);  
      return; 
    } 

    Type tt = null; 
    Type elemType = null; 

    if (typ != null) 
    { tt = Type.getTypeFor(typ,types,entities); 
      if (tt == null) 
      { if ("int".equals(typ) || "long".equals(typ) || typ.equals("String") || 
            typ.equals("Set") || typ.equals("Sequence") ||
            typ.equals("double") || typ.equals("boolean"))
        { tt = new Type(typ,null); }
        else  
        { System.err.println("Invalid type name: " + typ);
          tt = null;
        }
      }
      else 
      { elemType = tt.getElementType(); } 
    } 

    String params = opDialog.getParams(); // pairs var type
    Vector oppars = 
      BehaviouralFeature.reconstructParameters(params,types,entities);
    String pre = opDialog.getPre(); 
    String post = opDialog.getPost(); 

    Expression cond;
    Compiler2 comp = new Compiler2(); 
    if (pre == null || pre.equals(""))
    { cond = new BasicExpression("true"); } 
    else
    { comp.nospacelexicalanalysis(pre);
      cond = comp.parse();
      if (cond == null)
      { JOptionPane.showMessageDialog(null, "Error: invalid precondition: " + pre, 
                                      "",JOptionPane.ERROR_MESSAGE);  
        cond = new BasicExpression("true"); 
      } 
    }
    Expression spre = cond.simplify(); 

    Vector contexts = new Vector(); 
    // contexts.add(ent); 
    Vector vars = new Vector(); 
    vars.addAll(oppars);

    Attribute resultvar = null;  
    if (tt != null)
    { resultvar = new Attribute("result",tt,ModelElement.INTERNAL); 
      resultvar.setElementType(elemType); 
      vars.add(resultvar); 
    } 

    boolean tc = spre.typeCheck(types,entities,contexts,vars);
    if (!tc) 
    { System.err.println("Failed to type-check precondition"); }

    if (post == null)
    { System.err.println("Invalid postcondition"); 
      post = "true"; 
    }

    Compiler2 comp1 = new Compiler2(); 
    comp1.nospacelexicalanalysis(post); 
    Expression effect = comp1.parse(); 

    while (effect == null)
    { System.err.println("ERROR: Invalid postcondition syntax " + post); 
      JOptionPane.showMessageDialog(null, "Error: invalid postcondition syntax: " + post, 
                                      "",JOptionPane.ERROR_MESSAGE);  
      opDialog.setOldFields(nme,typ,params,pre,post,query);
      // opDialog.setStereotypes(null); 
      opDialog.setVisible(true);
      post = opDialog.getPost(); 
      query = opDialog.getQuery(); 
      comp1 = new Compiler2(); 
      comp1.nospacelexicalanalysis(post); 
      effect = comp1.parse();   
    } // loop until valid. 
    // Expression spost = effect.simplify(); 

    if (query) 
    { if (tt == null) 
      { System.err.println("Error: query operation must have a return type!"); 
        JOptionPane.showMessageDialog(null, "Error: no return type!", 
                                      "",JOptionPane.ERROR_MESSAGE);  
      } 
    } 
    else 
    { if (tt != null)
      { System.err.println("Warning: update operations with results cannot be mapped to B"); 
      } 
    } 

    // add op to entity, so it can be used recursively in its own post:
    BehaviouralFeature op = new BehaviouralFeature(nme,oppars,query,tt);
    op.setElementType(elemType); 
    if (resultvar != null) 
    { resultvar.setElementType(op.getElementType()); } 
 
    uc.addOperation(op);
    op.setUseCase(uc); 

    boolean tc2 = effect.typeCheck(types,entities,contexts,vars); 
    if (!tc2) 
    { System.err.println("Failed to type-check postcondition"); }
    else 
    { System.out.println("Definedness condition: " + effect.definedness()); 
      System.out.println("Determinacy condition: " + effect.determinate()); 
    } 

    op.setPre(spre); 
    op.setPost(effect); 
    String stereo = opDialog.getStereotypes(); 
    if (stereo != null && !(stereo.equals("none")))
    { op.addStereotype(stereo);
      if (stereo.equals("static"))
      { op.setInstanceScope(false); }
    } 
  }

  private void editUseCaseOperation(UseCase uc)
  { ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    Vector allops = uc.getOperations(); 
    listShowDialog.setOldFields(allops);
    System.out.println("Select operation to edit");
    Vector stereos = new Vector(); 
    listShowDialog.setVisible(true); 

    Object[] vals = listShowDialog.getSelectedValues();
    
    if (vals == null) { return; } 


    BehaviouralFeature op1 = null; 
    
    if (vals != null && vals.length > 0)
    { op1 = (BehaviouralFeature) vals[0];
        
      if (op1 == null) { return; } 
    } 

    if (opDialog == null)
    { opDialog = new OperationEditDialog(parent);
      opDialog.pack();
      opDialog.setLocationRelativeTo(this);
    }
    opDialog.setOldFields(op1.getName(),
           op1.getResultType() + "",op1.getParList(),op1.getPre() + "",
           op1.getPost() + "",op1.isQuery());
    opDialog.setVisible(true);
   
    String nme = opDialog.getName(); 
    String typ = opDialog.getType(); 
    boolean query = opDialog.getQuery(); 

    if (nme == null)
    { System.err.println("No name specified"); 
      return; 
    } 

    if (typ == null && query) 
    { System.err.println("Error: query operation without type"); 
      return; 
    } 

    Type elemType = null; 

    Type tt = null; 
    if (typ != null) 
    { tt = Type.getTypeFor(typ,types,entities); 
      if (tt == null) 
      { tt = new Type(typ,null); }
      elemType = tt.getElementType(); 
    } 

    String params = opDialog.getParams(); // pairs var type
    Vector oppars = 
      BehaviouralFeature.reconstructParameters(params,types,entities);
    String pre = opDialog.getPre(); 
    String post = opDialog.getPost(); 

    Expression cond;
    Compiler2 comp = new Compiler2(); 
    if (pre == null || pre.equals(""))
    { cond = new BasicExpression("true"); } 
    else
    { comp.nospacelexicalanalysis(pre);
      cond = comp.parse();
      if (cond == null)
      { System.err.println("ERROR: Syntax error in " + pre); 
        cond = new BasicExpression("true"); 
      } 
    }
    Expression spre = cond.simplify(); 

    Vector contexts = new Vector(); 
    // contexts.add(ent); 
    Vector vars = new Vector(); 
    vars.addAll(oppars);

    Attribute resultvar = null;  
    if (tt != null)
    { resultvar = new Attribute("result",tt,ModelElement.INTERNAL); 
      resultvar.setElementType(elemType); 
      vars.add(resultvar); 
    } 

    boolean tc = spre.typeCheck(types,entities,contexts,vars);
    if (!tc) 
    { System.err.println("Warning: Unable to type-check precondition " + cond); }

    if (post == null)
    { System.err.println("ERROR: Invalid postcondition"); 
      post = "true"; 
    }

    Compiler2 comp1 = new Compiler2(); 
    comp1.nospacelexicalanalysis(post); 
    Expression effect = comp1.parse(); 

    while (effect == null)
    { System.err.println("ERROR: Invalid postcondition syntax " + post); 
      JOptionPane.showMessageDialog(null, "Error: invalid postcondition " + post, 
                                      "",JOptionPane.ERROR_MESSAGE);  
      opDialog.setOldFields(nme,typ,params,pre,post,query);
      // opDialog.setStereotypes(null); 
      opDialog.setVisible(true);
      post = opDialog.getPost(); 
      query = opDialog.getQuery(); 
      comp1 = new Compiler2(); 
      comp1.nospacelexicalanalysis(post); 
      effect = comp1.parse();          
    }
    // Expression spost = effect.simplify(); 

    if (query) 
    { if (tt == null) 
      { System.err.println("Error: query operation must have a return type!"); 
        JOptionPane.showMessageDialog(null, "Error: no return type!", 
                                      "",JOptionPane.ERROR_MESSAGE);  
      } 
    } 
    else 
    { if (tt != null)
      { System.err.println("Warning: update operations with results cannot be mapped to B"); 
      } 
    } 

    // add op to entity, so it can be used recursively in its own post:
    BehaviouralFeature op = new BehaviouralFeature(nme,oppars,query,tt);
    if (tt != null)
    { op.setElementType(elemType); } 
    if (resultvar != null) 
    { resultvar.setElementType(op.getElementType()); } 

    uc.removeOperation(op1);  
    uc.addOperation(op);
    op.setUseCase(uc); 

    boolean tc2 = effect.typeCheck(types,entities,contexts,vars); 
    if (!tc2) 
    { System.err.println("Warning: Unable to type-check postcondition " + effect); 
      JOptionPane.showMessageDialog(null, "Cannot type-check postcondition " + effect, "", 
                                    JOptionPane.ERROR_MESSAGE);  
    }
    else 
    { System.out.println("Definedness condition: " + effect.definedness()); 
      System.out.println("Determinacy condition: " + effect.determinate()); 
    } 

    op.setPre(spre); 
    op.setPost(effect); 
    String stereo = opDialog.getStereotypes(); 
    if (stereo != null && !(stereo.equals("none")))
    { op.addStereotype(stereo);
      if (stereo.equals("static"))
      { op.setInstanceScope(false); }
    } // must always be static

    uc.resetDesign(); 
  }

  private void removeOperationFromUseCase(UseCase uc)
  { // open up list of operations from uc
    ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    Vector allops = uc.getOperations(); 

    listShowDialog.setOldFields(allops);
    // ((UmlTool) parent).thisLabel.setText("Select operations to delete"); 
    System.out.println("Select operations to delete");

    listShowDialog.setVisible(true); 

    Object[] vals = listShowDialog.getSelectedValues();
    if (vals != null && vals.length > 0)
    { for (int i = 0; i < vals.length; i++) 
      { System.out.println("Deleting " + vals[i]);
        BehaviouralFeature op = (BehaviouralFeature) vals[i];
        uc.removeOperation(op);
      }  
    } 

    uc.resetDesign(); 
  } 

  private void removeAttributeFromUseCase(UseCase uc)
  { String nme = JOptionPane.showInputDialog("Enter name of attribute:");; 
    if (nme == null) { return; } 
    uc.removeAttribute(nme);
    uc.resetDesign(); 
  }


  public UseCase expandUseCase(UseCase uc)
  { Vector pars = uc.getParameters(); 
    Vector exts = uc.getExtend(); 
    Vector incs = uc.getInclude(); 

    if (pars.size() == 0 && exts.size() == 0 && incs.size() == 0) 
    { return uc; } 

    UseCase newuc; 
    
    if (pars.size() > 0)
    { Vector avals = new Vector(); 
      System.out.println("Define actual values for parameters " + pars); 
      for (int i = 0; i < pars.size(); i++)
      { String apar = 
          JOptionPane.showInputDialog("Enter actual value of " + pars.get(i) + ":");
        Compiler2 comp = new Compiler2(); 
        comp.nospacelexicalanalysis(apar); 
        Expression parexp = comp.parse();
        if (parexp == null) 
        { System.err.println("Invalid parameter expression: " + parexp); } 
        // else  
        // { boolean b = parexp.typeCheck(types,entities,new Vector(),new Vector()); 
        //  if (b) 
        //  { System.out.println("Expression type-checked correctly"); 
            avals.add(parexp); 
        //  } 
        // } 
      } 
      newuc = uc.instantiate(avals,types,entities,associations); 
      // newuc.analyseConstraints(); 
    } 
    else 
    { newuc = (UseCase) uc.clone(); } 

    useCases.add(newuc);   // But not visually present
    uc.setName(newuc.getName() + "_expanded"); 
    uc.setDerived(true); // Not used for design or code, nor saved

    if (exts.size() > 0)
    { for (int k = 0; k < exts.size(); k++) 
      { Extend ex = (Extend) exts.get(k); 
        UseCase exuc = ex.getExtension(); 
        UseCase newexuc = expandUseCase(exuc); 
        newuc = Extend.insertIntoBase(newuc,newexuc,associations); 
      } 
    } 
    newuc.setExtend(new Vector()); 

    if (incs.size() > 0)
    { for (int k = 0; k < incs.size(); k++) 
      { Include ic = (Include) incs.get(k); 
        UseCase icuc = ic.getInclusion(); 
        UseCase newicuc = expandUseCase(icuc); 
        newuc = Include.insertIntoBase(newuc,newicuc); 
      } 
    } 
    newuc.setInclude(new Vector()); 

    // newuc.analyseConstraints(); 
        
    return newuc;         
  } 

  public void addInvariant(Invariant inv)
  { System.out.println("Select associations or entity that invariant is attached to");
    System.out.println("And/or the use case to which it belongs as a postcondition"); 
    String anames = 
      JOptionPane.showInputDialog("Enter association names, or entity, or entity + use case:");
    if (anames == null) 
    { return; } 
    StringTokenizer st = 
          new StringTokenizer(anames); 
    Vector astv = new Vector();
    Entity owner = null; 
    UseCase usecase = null; 
    int constraintType = 1; 
    String preEntity = ""; 
    int atindex = -1; 

    while (st.hasMoreTokens())
    { String se = st.nextToken().trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        se = se.substring(0,atindex); 
      } 

      Association ast = 
        (Association) ModelElement.lookupByName(se,associations); 
      if (ast != null) 
      { // System.out.println("Found association: " + ast);
        astv.add(ast);
      } 
      else 
      { // System.out.println("Unknown association: " + se);
        Entity e = (Entity) ModelElement.lookupByName(se,entities); 
        if (e != null)
        { owner = e; } 
        else 
        { UseCase uc = (UseCase) ModelElement.lookupByName(se,useCases); 
          if (uc != null) 
          { // System.out.println("Found use case");
            usecase = uc;
          }
        }
      } 
    } 

    Vector contexts = new Vector(); 
    if (owner != null) 
    { contexts.add(owner); }  

    boolean tc = inv.typeCheck(types,entities,contexts,new Vector()); 
    if (tc) 
    { // System.out.println("Invariant type-checked correctly"); 
      Constraint cons = new Constraint((SafetyInvariant) inv,astv); 
      for (int i = 0; i < astv.size(); i++) 
      { Association ast = (Association) astv.get(i); 
        ast.addConstraint(cons); 
      } 
      cons.setOwner(owner); 
      if (preEntity.length() > 0) 
      { cons.setisPre(true); } 
      cons.typeCheck(types,entities,contexts); // to identify variables
      // invariants.add(inv); 

      System.out.println("read frame of " + cons + " is " + cons.readFrame()); 

   
      if (usecase != null) 
      { usecase.addPostcondition(cons); 
        cons.setUseCase(usecase); 
        if (preEntity.length() > 0)
        { cons.setisPre(true); } 
        
      } 

      // constraints.add(cons); 
      
      boolean local = cons.checkIfLocal(); 
      if (usecase != null) { } 
      else if (local)
      { owner.addInvariant(cons); } 
      else  
      { constraints.add(cons);   // global constraint not in a use case

        // Vector ents = cons.innermostEntities();
        // System.out.println("Needed entities are: " + ents);   
        // Get the innermostEntities of con. 
        // Make sure each is listed by itself, or is source or dest
        // of some association in assocs. 
        Vector baseEntities = cons.innermostEntities();
        Vector endPoints = Association.getEndpoints(astv);
        Vector reachable = Association.reachableFrom(baseEntities,astv); 

        if (owner != null)
        { endPoints.add(owner); } 
        if (VectorUtil.subset(baseEntities,endPoints)) { }
        else
        { System.out.println("Warning, base entities: " + baseEntities +
                             " not subset of association ends: " + endPoints);
        }
        baseEntities.removeAll(reachable); 
        if (owner != null) 
        { Vector supers = owner.getAllSuperclasses(); 
          baseEntities.removeAll(supers);
        } 
        cons.setNeeded(baseEntities); 
        System.out.println("Needed entities for " + cons + 
                           " are " + baseEntities); 
        for (int y = 0; y < baseEntities.size(); y++) 
        { Entity ey = (Entity) baseEntities.get(y); 
          Association newa = findSubclassAssociation(ey,astv); 
          if (newa != null) 
          { if (astv.contains(newa)) { } 
            else 
            { astv.add(newa); } 
            System.out.println("Found subclass assoc: " + newa); 
          } // remove the ancestor assoc.
          else 
          { newa = findSubclass2Association(ey,astv); 
            if (newa != null) 
            { if (astv.contains(newa)) { }
              else 
              { astv.add(newa); }
              System.out.println("Found subclass assoc: " + newa); 
            }
          }
        } 
        cons.setAssociations(astv);  
        
        /* Vector assocs = relatedAssociations(feats); // checks names
        for (int j = 0; j < assocs.size(); j++) 
        { Association aa = (Association) assocs.get(j); 
          if (astv.contains(aa)) { } 
          else 
          { System.out.println("Association " + aa + " missing from constraint"); 
            cons.addAssociation(aa); 
          } 
        } */
      }
    } 
    else 
    { System.out.println("ERROR: Invariant not correctly typed, not added: " + inv); } 
  } 

  public void addInvariant(Invariant inv, Entity owner)
  { Vector contexts = new Vector(); 
    if (owner != null)
    { contexts.add(owner); } 

    boolean tc = inv.typeCheck(types,entities,contexts,new Vector()); 
    if (tc) 
    { // System.out.println("Invariant type-checked correctly"); 
      Constraint cons = new Constraint((SafetyInvariant) inv,
                                       new Vector()); 
      cons.setOwner(owner); 
      // invariants.add(inv); 
   
      // constraints.add(cons); 
      boolean local = cons.checkIfLocal(); 
      if (local)
      { owner.addInvariant(cons); } 
      else 
      { constraints.add(cons); 
        Vector baseEntities = cons.innermostEntities();
        // Vector reachable = Association.reachableFrom(baseEntities,astv); 

        Vector endPoints = new Vector();
        if (owner != null)
        { endPoints.add(owner); } 
        if (VectorUtil.subset(baseEntities,endPoints)) { }
        else
        { System.out.println("Warning, base entities: " + baseEntities +
                             " not subset of association ends: " + endPoints);
        }
      }
    } 
    else 
    { System.out.println("Invariant not correctly typed, not added: " + inv); 
      JOptionPane.showMessageDialog(null, "Invariant not correctly typed: " + inv, 
                                    "", JOptionPane.WARNING_MESSAGE);  
    } 
  } 

  public void removeInvariant(Constraint con)
  { constraints.remove(con); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.removeConstraint(con); 
    } 
  } 


  public void removeConstraint(Constraint con) 
  { constraints.remove(con); } 

  private Vector relatedAssociations(Vector features)
  { Vector res = new Vector();
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      Entity ent1 = ast.getEntity1();
      Entity ent2 = ast.getEntity2();
      Vector f1 = ent1.allFeatures();
      Vector f2 = ent2.allFeatures();
      f1.retainAll(features);
      f2.retainAll(features); 
      if (f1.size() > 0 || f2.size() > 0)
      { res.add(ast); }
    }
    return res;
  }

  public void addContrapositives()
  { Vector res = new Vector(); 
    for (int i = 0; i < constraints.size(); i++) 
    { Constraint con = (Constraint) constraints.get(i); 
      SafetyInvariant inv = new SafetyInvariant(con.antecedent(),
                                                con.succedent()); 
      Vector contras = SafetyInvariant.genAllContrapositives(inv); 
      for (int j = 0; j < contras.size(); j++) 
      { SafetyInvariant newinv = (SafetyInvariant) contras.get(j); 
        System.out.println("New invariant: " + newinv); 
        Vector context = new Vector(); 
        context.add(con.getOwner()); 
        boolean tc = newinv.typeCheck(types,entities,context,new Vector()); 
        // calculate modality
        Constraint newcon = new Constraint(newinv,con.getAssociations()); 
        if (constraints.contains(newcon)) { } 
        else 
        { newcon.setLocal(con.isLocal());
          newcon.setOwner(con.getOwner());
          res.add(newcon);
        }
      } 
    }
    constraints.addAll(res); 
    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      ent.addContrapositives(entities,types); 
    } 
  } 

  public void addTransitiveComps()
  { invariants.addAll(constraints); // these are used for B generation
    System.out.println("*** GENERATING TRANSITIVE CLOSURES OF INVARIANTS ***");
    System.out.println("*** Apply before B Generation **********************"); 
    Vector res = new Vector(); 
    for (int i = 0; i < constraints.size(); i++) 
    { Constraint con = (Constraint) constraints.get(i); 
      for (int j = i+1; j < constraints.size(); j++) 
      { Constraint con2 = (Constraint) constraints.get(j);
        SafetyInvariant inv = new SafetyInvariant(con.antecedent(), 
                                                  con.succedent()); 
        SafetyInvariant inv2 = new SafetyInvariant(con2.antecedent(), 
                                                   con2.succedent());  
        SafetyInvariant newinv = SafetyInvariant.transitiveComp(inv,inv2); 
        if (newinv != null)
        { if (newinv.isTrivial())
          { System.out.println("Trivial invariant: " + newinv); } 
          else 
          { System.out.println("Non-trivial invariant: " + newinv); 
            Vector contexts = new Vector(); 
            contexts.add(con.getOwner()); 
            newinv.typeCheck(types,entities,contexts,new Vector());
            Vector ass1 = con.getAssociations(); 
            Vector newass = VectorUtil.union(ass1,con2.getAssociations());  
            System.out.print("New invariant: " + newinv); 
            System.out.println(" on associations " + newass); 
            Constraint newcon = new Constraint(newinv,newass); 
            if (invariants.contains(newcon)) { } 
            else 
            { newcon.setLocal(false); 
              newcon.setOwner(con.getOwner()); 
            // newcon.typeCheck(types,entities); 
              res.add(newcon); 
            } 
          }
        }
      } 
    }
    invariants.addAll(res); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.addTranscomps(entities,types); 
    } 
  }  // also add inv comp inv' for inv: constraints and inv': e.invariants each e 

  public void printOCL()
  { for (int i = 0; i < constraints.size(); i++) 
    { Constraint cons = (Constraint) constraints.get(i); 
      Constraint cc = (Constraint) cons.clone(); 
      System.out.println("OCL Form: " + cc.toOcl()); 
    } 
  }

  public void listEntities()
  { System.out.println(entities); } 

  public void listInvariants()
  { System.out.println("********************************");
    System.out.println("*** GLOBAL INVARIANTS: ");
    for (int i = 0; i < constraints.size(); i++) 
    { Constraint cons = (Constraint) constraints.get(i); 
      System.out.println(cons); 
      // System.out.println("OCL Form: " + cons.toOcl()); 
    } 
    for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      System.out.println("*** LOCAL INVARIANTS OF ENTITY: " + e.getName()); 
      Vector einvs = e.getInvariants(); 
      for (int j = 0; j < einvs.size(); j++) 
      { Constraint inv = (Constraint) einvs.get(j); 
        System.out.println(inv); 
      }
    } 
  } 

  public Vector displayInvariants()
  { Vector res = new Vector(); 
    res.add("*** GLOBAL INVARIANTS: ");
    for (int i = 0; i < constraints.size(); i++) 
    { Constraint cons = (Constraint) constraints.get(i); 
      res.add(cons); 
    } 
    for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      res.add("*** LOCAL INVARIANTS OF ENTITY: " + e.getName()); 
      Vector einvs = e.getInvariants(); 
      res.addAll(einvs); 
    } 
    return res; 
  } 

  public Vector displayUseCaseInvariants(UseCase uc)
  { Vector res = new Vector();
    Vector assumptions = uc.getPreconditions(); 
 
    res.add("*** Assumptions: ");
    for (int i = 0; i < assumptions.size(); i++) 
    { Constraint cons = (Constraint) assumptions.get(i); 
      res.add(cons); 
    } 

    Vector postconditions = uc.getPostconditions(); 
    res.add("*** Postconditions: "); 
    for (int j = 0; j < postconditions.size(); j++) 
    { ConstraintOrGroup inv = (ConstraintOrGroup) postconditions.get(j); 
      res.add(inv); 
    } 
    // return res; 
    ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    // Vector allops = ent.getOperations(); 

    listShowDialog.setOldFields(res);
    // ((UmlTool) parent).thisLabel.setText("Select operations to delete"); 
    System.out.println("Select constraint");

    listShowDialog.setVisible(true); 

    Vector result = new Vector(); 

    Object[] vals = listShowDialog.getSelectedValues();
    if (vals != null && vals.length > 0)
    { for (int i = 0; i < vals.length; i++) 
      { System.out.println("Selected: " + vals[i]);
        Constraint op = (Constraint) vals[i];
        result.add(op);
      }  
    } 
    return result; 
  } 

  public Vector selectUseCase()
  { ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    // Vector allops = ent.getOperations(); 

    listShowDialog.setOldFields(useCases);
    System.out.println("Select use case");

    listShowDialog.setVisible(true); 

    Vector result = new Vector(); 

    Object[] vals = listShowDialog.getSelectedValues();
    if (vals != null && vals.length > 0)
    { for (int i = 0; i < vals.length; i++) 
      { System.out.println("Selected: " + vals[i]);
        UseCase op = (UseCase) vals[i];
        result.add(op);
      }  
    } 
    return result; 
  } 

  public Vector selectEntity()
  { ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    // Vector allops = ent.getOperations(); 

    listShowDialog.setOldFields(entities);
    System.out.println("Select entity:");

    listShowDialog.setVisible(true); 

    Vector result = new Vector(); 

    Object[] vals = listShowDialog.getSelectedValues();
    if (vals != null && vals.length > 0)
    { for (int i = 0; i < vals.length; i++) 
      { System.out.println("Selected: " + vals[i]);
        Entity op = (Entity) vals[i];
        result.add(op);
      }  
    } 
    return result; 
  } 

  public Vector selectOperation(Entity e)
  { ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    Vector allops = e.getOperations(); 

    listShowDialog.setOldFields(allops);
    System.out.println("Select operation:");

    listShowDialog.setVisible(true); 

    Vector result = new Vector(); 

    Object[] vals = listShowDialog.getSelectedValues();
    if (vals != null && vals.length > 0)
    { for (int i = 0; i < vals.length; i++) 
      { System.out.println("Selected: " + vals[i]);
        BehaviouralFeature op = (BehaviouralFeature) vals[i];
        result.add(op);
      }  
    } 
    return result; 
  } 

  public Vector selectAttribute(Entity e)
  { ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    Vector allops = e.getAttributes(); 

    listShowDialog.setOldFields(allops);
    System.out.println("Select attribute:");

    listShowDialog.setVisible(true); 

    Vector result = new Vector(); 

    Object[] vals = listShowDialog.getSelectedValues();
    if (vals != null && vals.length > 0)
    { for (int i = 0; i < vals.length; i++) 
      { System.out.println("Selected: " + vals[i]);
        Attribute op = (Attribute) vals[i];
        result.add(op);
      }  
    } 
    return result; 
  } 

  public void moveOperation()
  { JOptionPane.showMessageDialog(null, "Select source entity, then operation, then target entity",
                                  "Move operation", JOptionPane.INFORMATION_MESSAGE); 
    Vector ents = selectEntity(); 
    if (ents.size() == 0) { return; } 
    Entity src = (Entity) ents.get(0);
    Vector ops = selectOperation(src); 
    if (ops.size() == 0) { return; } 
    BehaviouralFeature op = (BehaviouralFeature) ops.get(0); 
    Vector trgz = selectEntity(); 
    if (trgz.size() == 0) { return; } 
    Entity trg = (Entity) trgz.get(0); 
    src.removeOperation(op); 
    trg.addOperation(op); 
    System.out.println("Moved operation " + op + " from " + src + " to " + trg); 
    repaint(); 
  } 

  public void moveAttribute()
  { JOptionPane.showMessageDialog(null, "Select source entity, then attribute, then target entity",
                                  "Move attribute", JOptionPane.INFORMATION_MESSAGE); 
    Vector ents = selectEntity(); 
    if (ents.size() == 0) { return; } 
    Entity src = (Entity) ents.get(0);
    Vector ops = selectAttribute(src); 
    if (ops.size() == 0) { return; } 
    Attribute att = (Attribute) ops.get(0); 
    Vector trgz = selectEntity(); 
    if (trgz.size() == 0) { return; } 
    Entity trg = (Entity) trgz.get(0); 
    src.removeAttribute(att.getName()); 
    trg.addAttribute(att); 
    System.out.println("Moved attribute " + att + " from " + src + " to " + trg); 
    repaint(); 
  } 

  public void displayActivities()
  { for (int i = 0; i < activities.size(); i++)
    { Behaviour bh = (Behaviour) activities.get(i); 
      System.out.println(bh); 
    } 
  } 


  public Dimension getPreferredSize() 
  { return preferredSize; }

  // public Vector getAllComponents() 
  // { Vector res = (Vector) sensors.clone();
    // return res.addAll(actuators); 
  // } 

  public void setEditMode(int emode)
  { editMode = emode;
    setAppropriateCursor(mode); 
  }

  public void setView(int vw)
  { view = vw; 
    repaint(); 
  } 
    
  public void findSelected(int x, int y)
  { boolean found = false;

    for (int i = 0; i < visuals.size(); i++)
    { VisualData vd = (VisualData) visuals.elementAt(i);
      if (vd.isUnder(x,y) || vd.isUnderStart(x,y) || 
          vd.isUnderEnd(x,y))
      { selectedVisual = vd;
        found = true;
        selectedComponent = vd.getModelElement(); 
        break; // NEW
        // System.out.println("Selected component: " + vd.label); 
      }  
    }
    // if (!found)
    // { System.out.println("No selection"); }
  }

  private void deleteSelected()
  { visuals.remove(selectedVisual);

    if (selectedVisual != null) 
    { componentNames.remove(selectedVisual.label); 
      entities.remove(selectedComponent); 
    } 

    // ??: 
    if (selectedComponent != null)
    { componentNames.remove(selectedComponent.getName());
      if (selectedComponent instanceof Entity) 
      { removeEntity((Entity) selectedComponent); }  // remove linked associatons?
      else if (selectedComponent instanceof Type) 
      { types.remove(selectedComponent); } 
      else if (selectedComponent instanceof Association)
      { Association ast = (Association) selectedComponent; 
        String role1 = ast.getRole1(); 
        if (role1 != null && role1.length() > 0)
        { Entity ent2 = ast.getEntity2(); 
          Association invast = ent2.getRole(role1); 
          ent2.removeAssociation(invast); 
        } 
        ast.getEntity1().removeAssociation(ast); 
        associations.remove(ast); 
      } 
      else if (selectedComponent instanceof UseCase)
      { UseCase uc = (UseCase) selectedComponent; 
        deleteUseCase(uc); 
      } 
      else // generalisation
      { Generalisation gen = (Generalisation) selectedComponent;
        removeGeneralisation(gen); 
      } 
      // if an association, remove it from source
      // entity. If an entity, remove all incident
      // associations, and inheritances. 
    }
  }

  public void removeEntity(Entity ent)
  { entities.remove(ent); 
    Entity sup = ent.getSuperclass(); 
    if (sup == null) {} 
    else 
    { removeInheritance(ent,sup); } 
    Vector subs = ent.getSubclasses(); 
    for (int i = 0; i < subs.size(); i++) 
    { Entity sub = (Entity) subs.get(i); 
      removeInheritance(sub,ent); 
    } 
  } 

  public void removeInheritance(Entity sub, Entity sup)
  { Vector removed = new Vector(); 
    for (int i = 0; i < generalisations.size(); i++)
    { Generalisation gen = (Generalisation) generalisations.get(i); 
      if (gen.getAncestor() == sup && gen.getDescendent() == sub)
      { removed.add(gen); } 
    } 
    for (int j = 0; j < removed.size(); j++) 
    { Generalisation gg = (Generalisation) removed.get(j); 
      removeGeneralisation(gg); 
      VisualData v = getVisualOf(gg); 
      visuals.remove(v); 
    } 
  } 

  public void removeGeneralisation(Generalisation gen)
  { Entity ans = gen.getAncestor(); 
    Entity dec = gen.getDescendent(); 
    if (ans.isInterface())
    { dec.removeInterface(ans); } 
    else 
    { dec.setSuperclass(null); 
      removeFamily(gen); 
    }
    ans.removeSubclass(dec); 
    // dec.removeSuperclass(ans); 
    generalisations.remove(gen);
  }

  public Vector getGeneralisations()
  { return generalisations; } 
    
  private void modifySelected()
  { if (selectedVisual == null || selectedComponent == null) 
    { return; } 

    if ((selectedVisual instanceof LineData) && 
        (selectedComponent instanceof Association))
    { Association ast = (Association) selectedComponent; 
      if (astDialog == null)
      { astDialog = new AstEditDialog(parent); 
        astDialog.pack();
        astDialog.setLocationRelativeTo(this);
      }

      System.out.println("Note that source and target entities cannot be changed"); 

      String oldrole1 = ast.getRole1(); 
      astDialog.setOldFields(ast.getName(),oldrole1,ast.getRole2(),
                             ModelElement.convertCard(ast.getCard1()),
                             ModelElement.convertCard(ast.getCard2()), 
                             ast.isOrdered(),ast.isFrozen(),
                             ast.isAddOnly());
      astDialog.setVisible(true);

      // if oldrole1 != null, also modify the inverse association. 

      if (astDialog.isCancel())
      { System.out.println("Modify cancelled");
        return; 
      } 
     
      String role2 = astDialog.getRole2(); 
        // JOptionPane.showInputDialog("Enter target role:");
      if (role2 == null)
      { System.out.println("Modify cancelled");
        return;
      }
      String card1 = astDialog.getCard1();
        // JOptionPane.showInputDialog("Source card: * or n or 0..1");    
      String card2 = astDialog.getCard2(); 
        // JOptionPane.showInputDialog("Target card: * or n or 0..1");
      int c1 = ModelElement.convertCard(card1);
      int c2 = ModelElement.convertCard(card2);
      String role1 = astDialog.getRole1(); 
      if ("".equals(role1))
      { role1 = null; } 

      ast.setRole2(role2); 
      ast.setRole1(role1); 
      if (c1 == ModelElement.AGGREGATION1)
      { ast.setCard1(ModelElement.ONE); 
        ast.setAggregation(true); 
        System.out.println("Defining aggregation"); 
      } 
      else if (c1 == ModelElement.AGGREGATION01)
      { ast.setCard1(ModelElement.ZEROONE); 
        ast.setAggregation(true); 
        System.out.println("Defining aggregation"); 
      } 
      else if (c1 == ModelElement.QUALIFIER)
      { ast.setCard1(ModelElement.MANY); // assume
        String qualatt = 
          JOptionPane.showInputDialog("Enter qualifier name:");
        Attribute qatt = new Attribute(qualatt,new Type("String",null),
                                       ModelElement.INTERNAL); 
        qatt.setElementType(new Type("String", null)); 
        ast.setQualifier(qatt); 
      }         
      else 
      { ast.setCard1(c1); } 
      ast.setCard2(c2); 
      ast.setOrdered(astDialog.getOrdered()); 
      ast.setName(astDialog.getName()); 
      ast.setFrozen(astDialog.getFrozen()); 
      ast.setAddOnly(astDialog.getAddOnly()); 
      String stereotypes = astDialog.getStereotypes(); 
      if (stereotypes != null && !(stereotypes.equals("none")))
      { ast.addStereotype(stereotypes); } 
      if (oldrole1 == null && role1 != null)
      { Association invast = ast.generateInverseAssociation(); 
        Entity ent2 = ast.getEntity2(); 
        ent2.addAssociation(invast);
      } 
      else if (oldrole1 != null)
      { Entity ent2 = ast.getEntity2(); 
        Association invast = ent2.getRole(oldrole1); 
        
        if (role1 == null) { ent2.removeAssociation(invast); }
        else
        { invast.setRole1(role2); 
          invast.setRole2(role1); 
          if (c2 == ModelElement.AGGREGATION1)
          { invast.setCard1(ModelElement.ONE); 
            invast.setAggregation(true); 
            System.out.println("Defining aggregation"); 
          } 
          else if (c2 == ModelElement.AGGREGATION01)
          { invast.setCard1(ModelElement.ZEROONE); 
            invast.setAggregation(true); 
            System.out.println("Defining aggregation"); 
          } 
          else 
          { invast.setCard1(c2); } 
          invast.setCard2(c1); 
          // invast.setName(astDialog.getName()); 
          String stereotypes2 = astDialog.getStereotypes(); 
          if (stereotypes2 != null && !(stereotypes2.equals("none")))
          { invast.addStereotype(stereotypes2); }
        } 
      }
      repaint(); 
      return; 
    } 

    if (selectedVisual instanceof OvalData)
    { editUseCase(selectedVisual.getName());
      return; 
    } 

    if ((selectedVisual instanceof RectData) && 
        (selectedComponent instanceof Entity))
    { Entity ent = (Entity) selectedComponent; 
      if (modifyDialog == null)
      { modifyDialog = new ModifyEntityDialog(parent); 
        modifyDialog.pack(); 
        modifyDialog.setLocationRelativeTo(this); 
      }
      modifyDialog.setVisible(true); 

      if (modifyDialog.isAddAtt())
      { addAttributeToEntity(ent); }
      else if (modifyDialog.isAddOp())
      { addOperationToEntity(ent); }  
      else if (modifyDialog.isRemoveAtt())
      { removeAttributeFromEntity(ent); } // may invalidate invariants -- 
                                          // re-typecheck
      else if (modifyDialog.isRemoveOp())
      { removeOperationFromEntity(ent); }  
      else if (modifyDialog.isEditOp())
      { editOperationFromEntity(ent); }  
      else if (modifyDialog.isEditName())
      { editEntityName(ent,(RectData) selectedVisual); }  
      else if (modifyDialog.isEditStereotypes())
      { editEntityStereotypes(ent); } 
      repaint(); 
      
      // give options: add attribute, add op, remove att, remove op, etc    
    } 

    if ((selectedVisual instanceof RectData) && 
        (selectedComponent instanceof Type))
    { Type typ = (Type) selectedComponent; 
      String vals = typ.getValuesAsString(); 
      String values =
          JOptionPane.showInputDialog("Enter values, separated by spaces:", vals);
      StringTokenizer st = 
          new StringTokenizer(values); 
      Vector elements = new Vector(); 
      while (st.hasMoreTokens())
      { String se = st.nextToken().trim();
        elements.add(se); 
      } 
      typ.setValues(elements); 
      repaint(); 
    } 

    selectedVisual = null; 
  } 

  public void createStatechart(Entity ent) 
  { Statemachine sm = state_win.makeNewWindow(ent.getName(),null); 
    sm.setEvents(ent); 
    ent.setStatechart(sm); 
  } 

  public void createStatechart(BehaviouralFeature bf) 
  { Statemachine sm = state_win.makeNewWindow(bf.getName(),null); 
    sm.setEvents(bf); 
    bf.setStatechart(sm); 
  } 

  private void addAttributeToEntity(Entity ent)
  { if (attDialog == null)
    { attDialog = new AttEditDialog(parent);
      attDialog.pack();
      attDialog.setLocationRelativeTo(this);
    }
    attDialog.setOldFields("","",ModelElement.INTERNAL,"",false,false,true);
    attDialog.setVisible(true);
   
    String nme = attDialog.getName(); 
    if (nme == null) { return; } 
    boolean alreadyDefined = ent.hasInheritedAttribute(nme); 
    if (alreadyDefined)
    { System.err.println("ERROR: Entity already has attribute " + nme + " not added"); 
      return; 
    }

    String typ = attDialog.getAttributeType();
    if (typ == null) 
    { System.err.println("null type, creation cancelled"); 
      return; 
    } 

    Type tt = Type.getTypeFor(typ, types, entities); 
    Type elemType = null; 
 
      // (Type) ModelElement.lookupByName(typ,types); 
    if (tt == null) 
    { if ("int".equals(typ) || "long".equals(typ) || typ.equals("String") || 
          typ.equals("double") || typ.equals("boolean"))
      { System.out.println("Inbuilt type: valid");
        tt = new Type(typ,null);
      }
      else
      { System.out.println("Warning: Invalid type name: " + typ);
        tt = new Type(typ,null);
      }
    } 
    else 
    { elemType = tt.getElementType(); } 

    int kind = attDialog.getKind(); 
    Attribute att = new Attribute(nme,tt,kind);
    att.setElementType(elemType); 
    att.setEntity(ent); 

    Vector contexts = new Vector(); 
    contexts.add(ent); 

    String ini = attDialog.getInit(); 
    if (ini != null && !ini.equals(""))
    { Compiler2 comp = new Compiler2();
      System.out.println("Initialisation: " + ini);
      comp.nospacelexicalanalysis(ini); 
      Expression initExp = comp.parse();
      if (initExp == null) 
      { System.err.println("ERROR: Invalid initialisation expression: " + ini); } 
      else  
      { boolean b = initExp.typeCheck(types,entities,contexts,new Vector()); 
        if (b) { } 
        else 
        { System.err.println("ERROR: Cannot type initialisation expression: " + ini); }

        Expression ie2 = initExp.simplify(); 
        String iqf = ie2.queryForm(new java.util.HashMap(),true);
            // should be to general language, not just Java4
        System.out.println("Initialisation: " + iqf);
        att.setInitialValue(iqf); 
        att.setInitialExpression(ie2); 
         // Should store Expression, not strings!   
      }
    }
    att.setFrozen(attDialog.getFrozen()); 
    att.setUnique(attDialog.getUnique()); 
    if (attDialog.getUnique() == true)
    { System.out.println("Primary key attribute. Must be of type String"); } 

    att.setInstanceScope(attDialog.getInstanceScope()); 
    ent.addAttribute(att);
  }

  private void editEntityName(Entity ent, RectData rd)
  { System.out.println("Enter new name. Existing expressions may become invalid"); 
    String newname = 
          JOptionPane.showInputDialog("Enter new name:");
    String oldName = ent.getName(); 
    if (newname != null && newname.trim().length() > 0)
    { ent.setName(newname.trim());
      rd.setName(newname.trim());
      changedEntityName(oldName, newname.trim()); 
      JOptionPane.showMessageDialog(null, "Existing constraints may be invalidated!", "", JOptionPane.WARNING_MESSAGE);
    } 
    else 
    { System.err.println("Invalid name: " + newname); }   
    repaint();   
  } 

  private void editEntityStereotypes(Entity ent)
  { System.out.println("Edit stereotypes: " + ent.getStereotypesString()); 
    String newstereotypes = 
          JOptionPane.showInputDialog("Enter new stereotypes:", ent.getStereotypesString());
    if (newstereotypes != null) 
    { ent.setStereotypes(newstereotypes); } 
  } 

  public void addUseCase()
  { if (ucDialog == null)
    { ucDialog = new UseCaseDialog(parent);
      ucDialog.pack();
      ucDialog.setLocationRelativeTo(this);
    }
    ucDialog.setOldFields("","","");
    ucDialog.setVisible(true);
   
    String nme = ucDialog.getName(); 
    String ent = ucDialog.getEntity(); 
    Entity e = (Entity) ModelElement.lookupByName(ent,entities); 
    if (e == null) 
    { System.err.println("Invalid entity name: " + ent); 
      return; 
    } 
    if (nme.equals("add") || nme.equals("remove") || nme.equals("create") ||
        nme.equals("delete") || nme.equals("edit") || nme.equals("get") ||
        nme.equals("list") || nme.equals("searchBy") || nme.equals("set"))
    { String name = nme + ent;
      String role = ucDialog.getRole(); 
      if (role != null && !(role.equals("")))
      { name = name + role; } 
      OperationDescription od = new OperationDescription(name,e,nme,role); 
      useCases.add(od); 
      System.out.println("New Use Case: " + od); 
    }  
  }

  public void addGeneralUseCase(UseCase uc)
  { useCases.add(uc); 
    OvalData od = new OvalData(10,80*useCases.size(),getForeground(),useCases.size()); 
    od.setName(uc.getName()); 
    od.setModelElement(uc); 
    visuals.add(od); 
    Entity e = uc.getClassifier(); 
    if (e != null) 
    { entities.add(e); } 
  }

  public void addGeneralUseCase()
  { if (ucDialog == null)
    { ucDialog = new UseCaseDialog(parent);
      ucDialog.pack();
      ucDialog.setLocationRelativeTo(this);
    }
    ucDialog.setOldFields("","","");
    ucDialog.setVisible(true);
   
    String nme = ucDialog.getName(); 
    if (nme == null || "".equals(nme)) { return; } 

    // String ent = ucDialog.getEntity(); 
    String desc = ucDialog.getDescription(); 
    String typ = ucDialog.getUseCaseType(); 

    if ("none".equals(typ)) 
    { typ = null; } 

    // Entity e = (Entity) ModelElement.lookupByName(ent,entities); 
    // if (e == null) 
    // { System.err.println("No entity specified"); } 

    UseCase uc = (UseCase) ModelElement.lookupByName(nme,useCases); 
    if (uc != null) 
    { System.err.println("ERROR: Existing use case with name " + nme); 
      return; 
    } 
    else 
    { System.out.println("New Use Case: " + nme);  
      uc = new UseCase(nme,null); 
      addGeneralUseCase(uc); 
      // useCases.add(uc); 
      // OvalData od = new OvalData(10,80*useCases.size(),getForeground(),useCases.size()); 
      // od.setName(nme); 
      // visuals.add(od); 
      repaint(); 
    } 

    uc.setDescription(desc); 
    Type tt = Type.getTypeFor(typ, types, entities); 
    uc.setResultType(tt); 
    if (tt != null) 
    { uc.setElementType(tt.getElementType()); }     
    
    /* String base = ucDialog.getExtends(); 
    UseCase ucbase = (UseCase) ModelElement.lookupByName(base,useCases); 
    if (ucbase != null) 
    { Extend ee = new Extend(ucbase,uc); 
      ucbase.addExtension(ee); 
      UseCase ucext = ee.insertIntoBase(); 
      System.out.println("Expanded Use Case: " + ucext);  
      
      useCases.add(ucext); 
    } */ 

    String pars = ucDialog.getParameters();
    if (pars == null || pars.trim().length() == 0) { } 
    else 
    { StringTokenizer partokens = new StringTokenizer(pars," "); 
      Vector patts = new Vector(); 
      while (partokens.hasMoreTokens())
      { String pp = partokens.nextToken();
        if (partokens.hasMoreTokens())
        { String ptype = partokens.nextToken();  
        // bind
          Type elemType = null; 
          Type ptt = Type.getTypeFor(ptype, types, entities); 
          if (ptt == null) 
          { System.err.println("ERROR: Invalid type for parameter: " + pp); }
          else       
          { Attribute pattr = new Attribute(pp, ptt, ModelElement.INTERNAL);
            pattr.setElementType(ptt.getElementType());  
            patts.add(pattr);
          } 
        } 
        else 
        { System.err.println("ERROR: missing type for parameter: " + pp); }
      } 
      uc.setParameters(patts); 
    }     
    repaint(); 
  }

  public void createBacktrackingSpecification(UseCase usec)
  { if (backtrackDialog == null)
    { backtrackDialog = new BacktrackDialog(parent);
      backtrackDialog.pack();
      backtrackDialog.setLocationRelativeTo(this);
    }
    usec.analyseConstraints(types, entities, associations); 
    Constraint con = (Constraint) usec.getPostcondition(1); 
    Entity en = con.getOwner(); 
    String noposs = ""; 
    Expression rng = con.getSecondaryQuantifier(0); 
    if (rng != null) 
    { noposs = "(" + rng + ")->size() = 0"; } 
    backtrackDialog.setOldFields(usec.getName(), "" + en, noposs, "" + rng);
    backtrackDialog.setVisible(true);
   
    String nme = backtrackDialog.getName(); 
    if (nme == null || "".equals(nme)) { return; } 

    String ent = backtrackDialog.getEntity(); 
    String undostats = backtrackDialog.getDescription(); // undo statements
    String scond = backtrackDialog.getUseCaseType();  // success condition
    String possiblevals = backtrackDialog.getParameters(); // possible values expression
    String bcond = backtrackDialog.getRole();  // backtrack condition

    Entity e = (Entity) ModelElement.lookupByName(ent,entities); 
    if (e == null) 
    { System.err.println("ERROR: No valid context entity specified: " + ent); 
      return; 
    } 

    UseCase uc = (UseCase) ModelElement.lookupByName(nme,useCases); 
    if (uc == null) 
    { System.err.println("ERROR: no valid use case with name: " + nme);
      return; 
    } 

    Compiler2 comp = new Compiler2();
    comp.nospacelexicalanalysis(bcond); 
    Expression bktrack = comp.parse();

    Compiler2 comp1 = new Compiler2();
    comp1.nospacelexicalanalysis(possiblevals); 
    Expression poss = comp1.parse();
      
    if (bktrack == null || poss == null) 
    { System.err.println("Invalid backtrack/possible values: " + bcond + " " + possiblevals); 
      return; 
    } 

    Compiler2 comp2 = new Compiler2(); 
    comp2.lexicalanalysis(undostats); 
    Statement undo = comp2.parseStatement(entities,types); 

    if (undo == null) 
    { System.err.println("Invalid undo statements: " + undostats); 
      return; 
    } 

    Compiler2 comp3 = new Compiler2();
    comp3.nospacelexicalanalysis(scond); 
    Expression success = comp3.parse();

    Vector contexts = new Vector(); 
    contexts.add(e); 
    poss.typeCheck(types,entities,contexts,new Vector());
    bktrack.typeCheck(types,entities,contexts,new Vector());
    undo.typeCheck(types,entities,contexts,new Vector());

    BacktrackingSpecification desc = new BacktrackingSpecification(e,uc,bktrack,poss,undo); 
    desc.setSuccess(success); 
    uc.setBacktrackingSpecification(desc); 
  }


  public void reconstructUseCase(String nme, String ent, String role)
  { Entity e = (Entity) ModelElement.lookupByName(ent,entities); 
    if (e == null) 
    { System.err.println("Invalid entity name: " + ent); 
      return; 
    } 
    if (nme.equals("add") || nme.equals("remove") || nme.equals("create") ||
        nme.equals("delete") || nme.equals("edit") || nme.equals("get") ||
        nme.equals("list") || nme.equals("searchBy") || nme.equals("set"))
    { String name = nme + ent;
      if (role != null && !(role.equals("")))
      { name = name + role; } 
      OperationDescription od = new OperationDescription(name,e,nme,role); 
      useCases.add(od); 
      System.out.println("New Use Case: " + od); 
    }  
  }

  public void listUseCases(PrintWriter out)
  { for (int i = 0; i < useCases.size(); i++)
    { ModelElement me = (ModelElement) useCases.get(i); 
      if (me instanceof OperationDescription) 
      { OperationDescription od = (OperationDescription) me; 
        System.out.println(od);
        out.println(od); 
      } 
      else if (me instanceof UseCase)
      { String disp = ((UseCase) me).display(); 
        System.out.println(disp);
        out.println(disp); 
      }  
    } 
  }

  public void displayMeasures(PrintWriter out)
  { out.println(); 
    out.println(); 
    System.err.println(); 
    System.err.println(); 
    java.util.Map clones = new java.util.HashMap(); 
    
    int highcost = 0; // cost of testability remediation in minutes
    int lowcost = 0;  // maintainability/changeability remediation

    int topscount = 0; 
    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      ent.displayMeasures(out,clones); 
      out.println(); 
      int eopscount = ent.operationsCount(); 

      if (eopscount > 20) 
      { lowcost = lowcost + 90*((eopscount - 10)/10); } 

      topscount = topscount + eopscount;

      int entehs = ent.excessiveOperationsSize(); 
      lowcost = lowcost + 30*entehs; 

      Vector ops = ent.getOperations(); 
      for (int k = 0; k < ops.size(); k++) 
      { BehaviouralFeature bf = (BehaviouralFeature) ops.get(k); 
        int cyc = bf.cc();
        if (cyc > 10) 
        { highcost = highcost + 30; } 

        int eplbf = bf.epl(); 
        if (eplbf > 10) 
        { highcost = highcost + 30; }

        int bfefo = bf.efo(); 
        if (bfefo > 5) 
        { highcost = highcost + 30; }
      }  
    } 

    int tcount = 0; 
    int trcount = 0; 
    int totalsize = 0; 

    for (int i = 0; i < useCases.size(); i++)
    { ModelElement me = (ModelElement) useCases.get(i); 
      if (me instanceof UseCase)
      { UseCase uc = (UseCase) me; 

        int ucsize = uc.displayMeasures(out,clones); 
        tcount++; 

        if (ucsize > 1000) 
        { lowcost = lowcost + 90*(ucsize/1000 - 1); } 

        int ucelementscount = uc.ruleCount() + uc.operationsCount(); 

        if (ucelementscount > 20) 
        { lowcost = lowcost + 90*((ucelementscount - 10)/10); } 

        trcount = trcount + uc.ruleCount(); 
        topscount = topscount + uc.operationsCount();

        totalsize = totalsize + ucsize;  
      }  
    } 


    Map cg = displayCallGraph(out,clones);

    int clonecount = 0; 
    java.util.Iterator keys = clones.keySet().iterator();
    while (keys.hasNext())
    { Object k = keys.next();
      Vector clonedIn = (Vector) clones.get(k); 
      if (clonedIn.size() > 1)
      { out.println("*** " + k + " is cloned in: " + clonedIn); 
        System.err.println("*** Bad smell: Cloned expression in " + clonedIn); 
        clonecount++; 
      } 
    }  

    highcost = highcost + 20*clonecount; 

    out.println("*** Total number of transformations in the system is: " + tcount);  
    out.println("*** Total number of transformation rules in the system is: " + trcount);  
    out.println("*** Total number of operations in the system is: " + topscount);  
    out.println("*** Total size of transformations in the system is: " + totalsize);  
    out.println("*** Total call graph size of transformation system is: " + cg.size());  
    out.println("*** Total number of clones in transformation system is: " + clonecount);  

    out.println(); 

    out.println("*** Estimated testability correction cost = " + highcost + " minutes (" + (highcost/60.0) + " hours)"); 
    out.println("*** Estimated maintainability correction cost = " + lowcost + " minutes (" + (lowcost/60.0) + " hours)"); 
  }

  public Map displayCallGraph(PrintWriter out, java.util.Map clones)
  { Map res = new Map(); 

    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      if (ent.isDerived()) { continue; } 

      Map cg = ent.getCallGraph(); 
      if (cg.size() > 0) 
      { out.println("*** Call graph of entity " + ent.getName() + " is: " + cg); 
        res = Map.union(res,cg); 
      }  
    } 

    // compute transitive closure of this

    Map tc = new Map(); 
    tc.elements = Map.transitiveClosure(res.elements);
    // out.println("Transitive closure of operations call graph is: " + tc);  

    Vector selfcalls = tc.getSelfMaps(); 
    int selfcallsn = selfcalls.size();  
 
    if (selfcallsn > 0) 
    { System.err.println("*** Bad smell: complex call graph with " + selfcallsn + " recursive dependencies"); } 


    Vector allused = new Vector(); 

    for (int i = 0; i < useCases.size(); i++)
    { ModelElement me = (ModelElement) useCases.get(i); 
      if (me instanceof UseCase)
      { UseCase uc = (UseCase) me; 
        String ucname = uc.getName(); 
        

        Map ucg = uc.getCallGraph(); 
        Vector rang = new Vector(); 

        if (ucg.size() > 0) 
        { out.println("*** use case " + me + " has " + ucg.size() + " operation calls:"); 
          out.println(ucg);
          Map transdependencies = Map.compose(ucg, tc);
          Map alldependencies = Map.union(ucg,transdependencies); 
          rang = alldependencies.range(); 
          if (rang.size() > 0) 
          { out.println("*** Use case " + me + " has dependencies upon: " + rang.size() + " operations"); } // range of this
          else 
          { out.println("*** " + me + " has no dependencies"); }
          
          if (rang.size() > 10) 
          { System.err.println("*** Bad smell: " + me + " uses too many operations: " + rang.size()); } 
          
          Map domrestr = Map.domainRestriction(rang,res); 
          int totalcgsize = domrestr.size() + ucg.size(); 
          out.println("*** Total call graph size of " + me + " is " + totalcgsize); 
          if (totalcgsize > uc.ruleCount() + uc.operationsCount() + rang.size()) 
          { System.err.println("*** Bad smell: " + me + " call graph too large: " + totalcgsize); } 

          Vector selfcallsuc = VectorUtil.intersection(selfcalls,rang); 
          int selfcallsucn = selfcallsuc.size(); 

          if (selfcallsucn > 0) 
          { out.println("*** " + selfcallsucn + " calls in recursive loops in " + me + " : " + selfcallsuc);  
            System.err.println("*** Bad smell: " + selfcallsucn + " calls in recursive loops in " + me + " : " + selfcallsuc); 
          } 


          allused = VectorUtil.union(allused,rang); 
 
          // get the total size of all the operations. 
          int opssize = 0; 
          for (int j = 0; j < rang.size(); j++) 
          { String dop = (String) rang.get(j); 
            opssize = opssize + getOperationSize(dop); 
          } 
          out.println("*** Total size of used entity operations in " + me + " is: " + opssize);
          out.println();  
          
          res = Map.union(res,ucg); 
        }  

        int ucclonecount = 0; 
        java.util.Iterator keys = clones.keySet().iterator();
        while (keys.hasNext())
        { Object k = keys.next();
          Vector clonedIn = (Vector) clones.get(k); 
          if (clonedIn.size() > 1)
          { String clonelocation = (String) clonedIn.get(0); 
            if (clonelocation.startsWith(ucname + "_"))
            { out.println(k + " is cloned in: " + ucname); 
              System.err.println("*** Bad smell: Cloned expression in " + ucname); 
              ucclonecount++;
            } 
            else if (rang.contains(clonelocation))
            { out.println("*** " + k + " is cloned in: " + ucname); 
              System.err.println("*** Bad smell: Cloned expression in " + ucname); 
              ucclonecount++;
            } 
          } 
        }  

        if (ucclonecount > 0) 
        { out.println("*** " + ucclonecount + " clones in " + me);  
          System.err.println("*** Bad smell: " + ucclonecount + " clones in " + me); 
          System.err.println(); 
          out.println(); 
        } 
      }  
    } 

    out.println("*** The transformation system uses " + allused.size() + " entity operations"); 
    int allusedsize = 0; 
    for (int j = 0; j < allused.size(); j++) 
    { String eop = (String) allused.get(j); 
      allusedsize = allusedsize + getOperationSize(eop); 
    } 
              
    out.println("*** The total size of all used entity operations is: " + allusedsize); 
    if (selfcallsn > 0) 
    { out.println("*** There are: " + selfcallsn + " operations involved in recursive loops"); } 
    else 
    { out.println("*** There are no operations involved in recursive loops"); } 
    out.println(); 

    return res; 
  }

  public int getOperationSize(String op)
  { int nind = op.indexOf("::"); 
    if (nind == 0) { return 0; } 
    String entname = op.substring(0,nind); 
    Entity ent = (Entity) ModelElement.lookupByName(entname,entities); 
    if (ent == null) { return 0; } 
    String opname = op.substring(nind+2, op.length()); 
    BehaviouralFeature bf = ent.getOperation(opname); 
    if (bf == null) 
    { return 0; } 
    return bf.syntacticComplexity(); 
  } 

  public String loadCSVModelOp()
  { String res = "  public static void loadCSVModel()\n" + 
      "  { boolean __eof = false;\n" +  
      "    String __s = \"\";\n" +  
      "    Controller __cont = Controller.inst();\n" +  
      "    BufferedReader __br = null;\n"; 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 

      if (ent.hasStereotype("target")) { continue; } 

      if (ent.isAbstract()) { continue; } 

      if (ent.isInterface()) { continue; } 

      if (ent.hasStereotype("auxilliary")) { continue; } 

      if (ent.hasStereotype("external")) { continue; } 

      if (ent.hasStereotype("externalApp")) { continue; } 

      String ename = ent.getName(); 
      String ex = ename.toLowerCase() + "x"; 
      String elower = "_" + ename.toLowerCase();
 
      res = res + 
          "    try\n" + 
          "    { File " + elower + " = new File(\"" + ename + ".csv\");\n" + 
          "      __br = new BufferedReader(new FileReader(" + elower + "));\n" +
          "      __eof = false;\n" +         
          "      while (!__eof)\n" + 
          "      { try { __s = __br.readLine(); }\n" + 
          "        catch (IOException __e)\n" + 
          "        { System.out.println(\"Reading failed.\");\n" + 
          "          return;\n" +  
          "        }\n" + 
          "        if (__s == null)\n" +  
          "        { __eof = true; }\n" + 
          "        else\n" +  
          "        { " + ename + " " + ex + " = " + ename + ".parseCSV(__s.trim());\n" +  
          "          if (" + ex + " != null)\n" + 
          "          { __cont.add" + ename + "(" + ex + "); }\n" +  
          "        }\n" +
          "      }\n" + 
          "    }\n" +   
          "    catch(Exception __e) { }\n";   
    } 
    res = res + "  }\n\n"; 
    return res;  
  } 

  public String saveCSVModelOp()
  { String res = "  public void saveCSVModel()\n" + 
      "  { try {\n";   

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 

      if (ent.isAbstract()) { continue; } 

      if (ent.isInterface()) { continue; } 

      if (ent.hasStereotype("auxilliary")) { continue; } 

      if (ent.hasStereotype("external")) { continue; } 

      if (ent.hasStereotype("externalApp")) { continue; } 

      String ename = ent.getName(); 
      String ex = ename.toLowerCase() + "x"; 
      String elower = "_" + ename.toLowerCase();
      String es = ename.toLowerCase() + "s"; 
      String eout = "_out_" + ename.toLowerCase();
 
      res = res + 
          "      File " + elower + " = new File(\"" + ename + ".csv\");\n" + 
          "      PrintWriter " + eout + " = new PrintWriter(new BufferedWriter(new FileWriter(" + elower + ")));\n" +        
          "      for (int __i = 0; __i < " + es + ".size(); __i++)\n" + 
          "      { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(__i);\n" + 
          "        " + ex + ".writeCSV(" + eout + ");\n" + 
          "      }\n" + 
          "      " + eout + ".close();\n";   
    } 
    res = res + "    }\n" +   
                "    catch(Exception __e) { }\n" + 
                "  }\n\n"; 
    return res;  
  } 

  // Need to do this for other languages, not just Java4

  public void saveCSV()
  { File chtml = new File("output/mm.csv"); 
    try
    { PrintWriter chout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(chtml)));
      
      for (int j = 0; j < entities.size(); j++) 
      { Entity ent = (Entity) entities.get(j); 
        String entvo = ent.toCSV();
        chout.println(entvo); 
      }  

      chout.close(); 
    } catch (Exception e) { } 

  }


  public void generateJSPWebSystem(PrintWriter out)
  { File chtml = new File("output/commands.html"); 
    try
    { PrintWriter chout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(chtml)));
      chout.println(generateCommandHtml(useCases)); 
      // out.println();
      chout.close(); 
    } catch (Exception e) { } 

    for (int i = 0; i < useCases.size(); i++)
    { Object obj = useCases.get(i); 
      if (obj instanceof OperationDescription)
      { OperationDescription od = (OperationDescription) obj; 
        String nme = od.getName(); 
        File odjsp = new File("output/" + nme + ".jsp"); 
        try
        { PrintWriter jspout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(odjsp)));
          jspout.println(od.getJsp()); 
          jspout.close(); 
        } catch (Exception e) { } 

      // out.println(od.getJsp());
      // out.println(); 
        File odhtml = new File("output/" + nme + ".html"); 
        try
        { PrintWriter odhout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(odhtml)));
          odhout.println(od.getInputPage());
          odhout.close(); 
        } catch (Exception e) { } 
      } 
    } 

    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      String entvo = ent.getName() + "VO.java"; 
      File entvof = new File("output/" + entvo); 
      try
      { PrintWriter voout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(entvof)));
        voout.println(ent.getValueObject());
        voout.close(); 
      } catch (Exception e) { } 
      
      String entbean = ent.getName() + "Bean.java"; 
      File entbeanf = new File("output/" + entbean); 
      try
      { PrintWriter beanout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(entbeanf)));
        beanout.println(ent.generateBean(useCases,constraints,entities,types));
        beanout.close(); 
      } catch (Exception e) { } 
    }

    File dbif = new File("output/Dbi.java"); 
    try
    { PrintWriter dbiout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(dbif)));
      dbiout.println(generateJspDbi(useCases));
      dbiout.close(); 
    } catch (Exception e) { } 

    out.println(generateDbiPool());  
  }

  public void generateJ2EEWebSystem(PrintWriter out)
  { for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      out.println(ent.getValueObject());
      out.println(); 
      out.println(ent.ejbBean());
      out.println();   
      out.println(ent.genEJBLocalRemote(true)); 
      out.println(); 
      out.println(ent.genEJBHome(true)); 
      out.println(); 
      out.println(ent.genEJBLocalRemote(false)); 
      out.println(); 
      out.println(ent.genEJBHome(false)); 
    }
    out.println(getSessionBeans(entities,useCases)); 
    // out.println(generateJspDbi(useCases));
    // out.println(generateDbiPool());  
  }


  public void generateWebSystem(PrintWriter out)
  { out.println(generateBasePage("Web System")); 
    if (useCases == null) { return; } 

    out.println(generateCommandPage(useCases)); 
    for (int i = 0; i < useCases.size(); i++)
    { Object obj = useCases.get(i); 
      if (!(obj instanceof OperationDescription)) { continue; } 

      OperationDescription od = (OperationDescription) obj; 
      String srvltcode = od.getServletCode(); 
      out.println(srvltcode);
      System.out.println(srvltcode); 
      String genclass = od.getGenerationClass();  
      out.println(genclass);  
    } 

    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      out.println(ent.getResultPage()); 
    }
    out.println(generateErrorPage()); 
    out.println(generateDbi(useCases)); 
  }

  public Type lookupType(String tname) 
  { Entity e = (Entity) ModelElement.lookupByName(tname,entities);
    if (e != null) 
    { return new Type(e); } 
    Type tt = (Type) ModelElement.lookupByName(tname,types); 
    if (tt != null) 
    { return tt; } 
    if ("int".equals(tname) || "long".equals(tname) || tname.equals("String") || 
         tname.equals("Set") || tname.equals("Sequence") ||
         tname.equals("double") || tname.equals("boolean"))
    { // System.out.println("Creating standard type " + tname);
      return new Type(tname,null);
    }
    // System.err.println("Invalid/unknown type: " + tname); 
    return new Type(tname, null); 
  } 
    

  private void addOperationToEntity(Entity ent)
  { if (opDialog == null)
    { opDialog = new OperationEditDialog(parent);
      opDialog.pack();
      opDialog.setLocationRelativeTo(this);
    }
    opDialog.setOldFields("","","","","",true);
    opDialog.setStereotypes(null); 
    opDialog.setVisible(true);
   
    String nme = opDialog.getName(); 
    String typ = opDialog.getType(); 
    boolean query = opDialog.getQuery(); 

    if (nme == null)  // cancelled 
    { System.err.println("Operation definition cancelled. No name specified"); 
      return; 
    } 

    if (typ == null && query) 
    { System.err.println("Error: query operation without type"); 
      JOptionPane.showMessageDialog(null, "ERROR: query operation must have a return type!", "", JOptionPane.ERROR_MESSAGE); 
      return; 
    } 

    Type tt = null; 
    Type elemType = null; 


    if (typ != null) 
    { Compiler2 tcompiler = new Compiler2(); 
      tcompiler.nospacelexicalanalysis(typ); 
      tt = tcompiler.parseType(entities,types); 
      if (tt != null) 
      { elemType = tt.getElementType(); } 
      // System.out.println("Result type " + tt + " found"); 
    } 

    String pre = opDialog.getPre(); 
    String post = opDialog.getPost(); 
    String params = opDialog.getParams(); // pairs var type

    Expression cond;
    Compiler2 comp = new Compiler2(); 
    if (pre == null || pre.equals(""))
    { cond = new BasicExpression("true"); } 
    else
    { comp.nospacelexicalanalysis(pre);
      cond = comp.parse();
      if (cond == null)
      { System.err.println("Warning, precondition has wrong syntax: " + pre); 
        cond = new BasicExpression("true"); 
      } 
    }
    Expression spre = cond.simplify(); 

    Vector contexts = new Vector(); 
    contexts.add(ent); 
    Vector vars = new Vector(); 

    Attribute resultvar = null; 
    if (tt != null)
    { resultvar = new Attribute("result",tt,ModelElement.INTERNAL); 
      resultvar.setElementType(elemType); 
      vars.add(resultvar); 
    } 


    if (post == null)
    { System.out.println("Invalid postcondition"); 
      post = "true"; 
    }
    Compiler2 comp1 = new Compiler2(); 
    comp1.nospacelexicalanalysis(post); 
    Expression effect = comp1.parse(); 

    while (effect == null)
    { System.out.println("Invalid postcondition: " + post); 
      JOptionPane.showMessageDialog(null, "ERROR: invalid expression: " + post, "", JOptionPane.ERROR_MESSAGE); 
      opDialog.setOldFields(nme,typ,params,pre,post,query);
      // opDialog.setStereotypes(null); 
      opDialog.setVisible(true);
      post = opDialog.getPost();
      query = opDialog.getQuery(); 
      Compiler2 compx = new Compiler2(); 
      compx.nospacelexicalanalysis(post); 
      effect = compx.parse(); 
    }
    // Expression spost = effect.simplify(); 


    if (query) 
    { if (tt == null) 
      { System.err.println("Error: query operation must have a return type!");                JOptionPane.showMessageDialog(null, "ERROR: query operation must have a return type!", "", JOptionPane.ERROR_MESSAGE);
        return;  
      } 
    } 
    else 
    { if (tt != null)
      { System.err.println("Warning: update operations with results cannot be mapped to B"); 
      } 
    } 

    params = opDialog.getParams(); // pairs var type
    Vector oppars = 
      BehaviouralFeature.reconstructParameters(params,types,entities);
    vars.addAll(oppars); 

    boolean tc = spre.typeCheck(types,entities,contexts,vars);
    if (!tc) 
    { System.out.println("Warning: cannot type precondition: " + spre); 
      // spre = new BasicExpression("true"); 
      // return; 
    }

    // add op to entity, so it can be used recursively in its own post:
    BehaviouralFeature op = new BehaviouralFeature(nme,oppars,query,tt);
    if (tt != null)
    { op.setElementType(elemType); } 
    if (resultvar != null) 
    { resultvar.setElementType(op.getElementType()); } 

    ent.addOperation(op);

    boolean tc2 = effect.typeCheck(types,entities,contexts,vars); 
    if (!tc2) 
    { System.out.println("Warning: cannot type postcondition: " + effect); }
    else 
    { System.out.println("Definedness condition: " + effect.definedness()); 
      System.out.println("Determinacy condition: " + effect.determinate()); 
    } 

    op.setPre(spre); 
    op.setPost(effect); 
    String stereo = opDialog.getStereotypes(); 
    if (stereo != null && !(stereo.equals("none")))
    { op.addStereotype(stereo);
      if (stereo.equals("static"))
      { op.setInstanceScope(false); }
    } 
  }

  private void removeOperationFromEntity(Entity ent)
  { // open up list of operations from ent
    ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    Vector allops = ent.getOperations(); 

    listShowDialog.setOldFields(allops);
    // ((UmlTool) parent).thisLabel.setText("Select operations to delete"); 
    System.out.println("Select operations to delete");

    listShowDialog.setVisible(true); 

    Object[] vals = listShowDialog.getSelectedValues();
    if (vals != null && vals.length > 0)
    { for (int i = 0; i < vals.length; i++) 
      { System.out.println(vals[i]);
        BehaviouralFeature op = (BehaviouralFeature) vals[i];
        ent.removeOperation(op);
      }  
    } 
    JOptionPane.showMessageDialog(null, "Warning: design no longer valid!", "", JOptionPane.WARNING_MESSAGE);
    resetDesigns();  
  } 

  private void editOperationFromEntity(Entity ent)
  { // open up list of operations from ent
    ListShowDialog listShowDialog = new ListShowDialog(parent);
    listShowDialog.pack();
    listShowDialog.setLocationRelativeTo(parent); 
    
    Vector allops = ent.getOperations(); 

    listShowDialog.setOldFields(allops);

    System.out.println("Select operation to edit");


    Vector stereos = new Vector(); 

    listShowDialog.setVisible(true); 

    Object[] vals = listShowDialog.getSelectedValues();
    
    if (vals == null) { return; } 


    String nme = ""; 
    String typ = ""; 

    BehaviouralFeature op1 = null; 

    if (vals != null && vals.length > 0)
    { op1 = (BehaviouralFeature) vals[0]; } 
        
    if (op1 == null) { return; } 

    stereos.addAll(op1.getStereotypes()); 


    if (opDialog == null)
    { opDialog = new OperationEditDialog(parent);
      opDialog.pack();
      opDialog.setLocationRelativeTo(this);
    }
    opDialog.setOldFields(op1.getName(),
    op1.getResultType() + "",op1.getParList(),op1.getPre() + "",
    op1.getPost() + "",op1.isQuery());
    opDialog.setStereotypes(stereos); 

    opDialog.setVisible(true);
    nme = opDialog.getName(); 
    typ = opDialog.getType(); 

    if (nme == null)  // cancelled 
    { System.err.println("Edit cancelled. No name specified"); 
      return; 
    } 
    ent.removeOperation(op1);
 
    boolean query = opDialog.getQuery(); 

    if (nme == null)  // cancelled 
    { System.err.println("Operation definition cancelled. No name specified"); 
      return; 
    } 

    if (typ == null && query) 
    { System.err.println("Error: query operation without type"); 
      JOptionPane.showMessageDialog(null, "ERROR: query operation must have a return type!", "", JOptionPane.ERROR_MESSAGE); 
      return; 
    } 

    Type tt = null; 
    Type elemType = null; 

    if (typ != null) 
    { tt = Type.getTypeFor(typ, types, entities); 
      if (tt != null) 
      { elemType = tt.getElementType(); } 
      else 
      { System.err.println("Error: invalid operation type " + typ); }
    }      


    String params = opDialog.getParams(); // pairs var type
    Vector oppars = 
      BehaviouralFeature.reconstructParameters(params,types,entities);
    String pre = opDialog.getPre(); 
    String post = opDialog.getPost(); 

    Expression cond;
    Compiler2 comp = new Compiler2(); 
    if (pre == null || pre.equals(""))
    { cond = new BasicExpression(true); } 
    else
    { comp.nospacelexicalanalysis(pre);
      cond = comp.parse();
      if (cond == null)
      { System.err.println("Error: invalid precondition syntax: " + pre); 
        cond = new BasicExpression(true); 
      } 
    }
    Expression spre = cond.simplify(); 

    Vector contexts = new Vector();
    contexts.add(ent); 

    Vector vars = new Vector(); 
    vars.addAll(oppars); 
    Attribute resultvar = null;  
    if (tt != null)
    { resultvar = new Attribute("result",tt,ModelElement.INTERNAL); 
      resultvar.setElementType(elemType); 
      vars.add(resultvar); 
    } 

    boolean tc = spre.typeCheck(types,entities,contexts,vars);
    if (!tc) 
    { System.out.println("Warning: Cannot type precondition: " + spre); 
      spre = new BasicExpression(true); 
      // return; 
    }

    if (post == null)
    { System.out.println("Error: Invalid postcondition: " + post); 
      post = "true"; 
    }
    Compiler2 comp1 = new Compiler2(); 
    comp1.nospacelexicalanalysis(post); 
    Expression effect = comp1.parse(); 

    while (effect == null)
    { System.out.println("Invalid postcondition: " + post); 
      JOptionPane.showMessageDialog(null, "ERROR: invalid expression!: " + post, "", JOptionPane.ERROR_MESSAGE); 
      opDialog.setOldFields(nme,
           typ, params, pre,
           post, query);

      opDialog.setVisible(true);

      Compiler2 comp2 = new Compiler2();
      post = opDialog.getPost();  
      query = opDialog.getQuery(); 
      comp2.nospacelexicalanalysis(post); 
      effect = comp2.parse(); 
    }
    // Expression spost = effect.simplify(); 

    if (query) 
    { if (tt == null) 
      { System.err.println("ERROR: query operation must have a return type!");                 JOptionPane.showMessageDialog(null, "ERROR: query operation must have a return type!", "", JOptionPane.ERROR_MESSAGE); 
      }  
    } 
    else 
    { if (tt != null)
      { System.err.println("WARNING: update operations with results cannot be mapped to B"); 
      } 
    } 

    // add op to entity, so it can be used recursively in its own post:
    BehaviouralFeature op = new BehaviouralFeature(nme,oppars,query,tt);
    if (tt != null)
    { op.setElementType(elemType); } 
    if (resultvar != null) 
    { resultvar.setElementType(op.getElementType()); } 
 
    ent.addOperation(op);

    boolean tc2 = effect.typeCheck(types,entities,contexts,vars); 
    if (!tc2) 
    { System.out.println("Warning, cannot type postcondition: " + effect); }
    else 
    { System.out.println("Definedness condition: " + effect.definedness()); 
      System.out.println("Determinacy condition: " + effect.determinate()); 
    } 

    op.setPre(spre); 
    op.setPost(effect); 

    String stereo = opDialog.getStereotypes(); 
    op.setStereotypes(stereo); 
    if (stereo != null && !(stereo.equals("none")))
    { op.addStereotype(stereo);
      if (stereo.equals("static"))
      { op.setInstanceScope(false); }
    } 
  } 

  public void createEntityActivity(Entity ent)
  { if (actDialog == null)
    { actDialog = new ActivityEditDialog(parent);
      actDialog.pack();
      actDialog.setLocationRelativeTo(this);
    }
    actDialog.setOldFields("","","","","",true);
    actDialog.setVisible(true);
   
    String post = actDialog.getPost(); 
    Compiler2 comp = new Compiler2(); 
    if (post == null)
    { System.out.println(">>>>> Invalid activity: " + post); 
      return; 
    }
    comp.lexicalanalysis(post); 
    Statement effect = comp.parseStatement(entities,types); 

    while (effect == null)
    { System.out.println(">>>>>> Invalid activity code: " + post); 
      actDialog.setOldFields("","","","",post + "",true);
      actDialog.setVisible(true);
   
      post = actDialog.getPost(); 
      comp = new Compiler2(); 
      if (post == null)
      { System.out.println(">>>>> Invalid activity: " + post); 
        return; 
      }
      comp.lexicalanalysis(post); 
      effect = comp.parseStatement(entities, types);   
    }
    effect.setEntity(ent); 
    Vector contexts = new Vector(); 
    contexts.add(ent); 

    effect.typeCheck(types,entities,contexts,new Vector()); 
    Vector pres = effect.allPreTerms(); 
    // System.out.println("All pre-expressions used: " + pres); 
    ent.setActivity(effect); 
    System.out.println("Set activity for entity " + ent); 
    updateActivities(ent,effect); 
    // activities.add(new Behaviour(ent,effect)); 
  }

  public void editEntityActivity(Entity ent)
  { Statement stat = ent.getActivity(); 

    if (actDialog == null)
    { actDialog = new ActivityEditDialog(parent);
      actDialog.pack();
      actDialog.setLocationRelativeTo(this);
    }
    actDialog.setOldFields("","","","","" + stat,true);
    actDialog.setVisible(true);
   
    String post = actDialog.getPost(); 
    Compiler2 comp = new Compiler2(); 
    if (post == null)
    { System.out.println(">>>>>> Invalid activity"); 
      return; 
    }
    comp.lexicalanalysis(post); 
    Statement effect = comp.parseStatement(entities,types); 

    while (effect == null)
    { System.out.println(">>>>> Invalid activity code: " + post); 
      actDialog.setOldFields("","","","",post + "",true);
      actDialog.setVisible(true);
   
      post = actDialog.getPost(); 
      comp = new Compiler2(); 
      if (post == null)
      { System.out.println(">>>>> Invalid activity: " + post); 
        return; 
      }
      comp.lexicalanalysis(post); 
      effect = comp.parseStatement(entities,types);   
    }
    effect.setEntity(ent); 
    Vector contexts = new Vector(); 
    contexts.add(ent); 

    effect.typeCheck(types,entities,contexts,new Vector()); 
    Vector pres = effect.allPreTerms(); 
    // System.out.println("All pre-expressions used: " + pres); 
    ent.setActivity(effect); 
    System.out.println("Set activity for entity " + ent); 
    updateActivities(ent, effect); 
    // activities.add(new Behaviour(ent,effect)); 
  }

  public void createOperationActivity(Entity ent)
  { if (actDialog == null)
    { actDialog = new ActivityEditDialog(parent);
      actDialog.pack();
      actDialog.setLocationRelativeTo(this);
    }
    actDialog.setOldFields("","","","","",true);
    actDialog.setVisible(true);

    String nme = actDialog.getName(); 
    if (nme == null || nme.equals(""))
    { System.out.println("Enter an operation name from the entity"); 
      return; 
    }    
    String post = actDialog.getPost(); 
    Compiler2 comp = new Compiler2(); 
    if (post == null)
    { System.out.println(">>>>> ERROR: Invalid activity: " + post); 
      return; 
    }
    comp.nospacelexicalanalysis(post); 
    Statement effect = comp.parseStatement(entities,types); 

    while (effect == null)
    { System.out.println(">>>>> ERROR: Syntax error in activity: " + post); 
      actDialog.setOldFields(nme,"","","",post,true);
      actDialog.setVisible(true);
      post = actDialog.getPost(); 
      Compiler2 comp2 = new Compiler2(); 
      if (post == null)
      { System.out.println(">>>>> ERROR: Invalid activity: " + post); 
        return; 
      }
      comp2.nospacelexicalanalysis(post); 
      effect = comp2.parseStatement(entities,types); 
    }
    
    Vector contexts = new Vector(); 
    contexts.add(ent); 
    Vector pars = new Vector(); 

    effect.setEntity(ent); 
    BehaviouralFeature op = ent.getOperation(nme); 
    if (op != null) 
    { pars.addAll(op.getParameters()); } 

    effect.typeCheck(types,entities,contexts,pars); 
    Vector pres = effect.allPreTerms(); 
    // System.out.println("All pre-expressions used: " + pres); 

    if (op != null)
    { op.setActivity(effect); 
      System.out.println("Set activity for operation " + nme + " of entity " + ent); 
      updateActivities(ent, op, effect); 
      // activities.add(new Behaviour(ent,op,effect)); 
    }
  }

  public void editOperationActivity(Entity ent)
  { String nme = 
          JOptionPane.showInputDialog("Enter operation name:");
    BehaviouralFeature bf = ent.getOperation(nme); 
    if (bf == null) 
    { System.err.println("ERROR: No such operation: " + nme); 
      return; 
    } 
    Statement stat = bf.getActivity(); 

    if (actDialog == null)
    { actDialog = new ActivityEditDialog(parent);
      actDialog.pack();
      actDialog.setLocationRelativeTo(this);
    }
    actDialog.setOldFields(nme,"","","","" + stat,true);
    actDialog.setVisible(true);

    String post = actDialog.getPost(); 
    Compiler2 comp = new Compiler2(); 
    if (post == null)
    { System.out.println(">>>>> ERROR: Invalid activity: " + post); 
      return; 
    }
    comp.nospacelexicalanalysis(post); 
    Statement effect = comp.parseStatement(entities,types); 

    while (effect == null)
    { System.err.println(">>>>> ERROR: Syntax error in activity: " + post); 
      actDialog.setOldFields(nme,"","","",post,true);
      actDialog.setVisible(true);
      post = actDialog.getPost(); 
      Compiler2 comp2 = new Compiler2(); 
      if (post == null)
      { System.out.println(">>>>> ERROR: Invalid activity: " + post); 
        return; 
      }
      comp2.nospacelexicalanalysis(post); 
      effect = comp2.parseStatement(entities,types); 
    }
    
    Vector contexts = new Vector(); 
    contexts.add(ent); 
    Vector pars = new Vector(); 

    effect.setEntity(ent); 
    pars.addAll(bf.getParameters()); 

    effect.typeCheck(types,entities,contexts,pars); 
    /* Vector pres = effect.allPreTerms(); 
       System.out.println("All pre-expressions used: " + pres); */ 

    bf.setActivity(effect); 
    updateActivities(ent, bf, effect); 
    System.out.println("Set activity for operation " + nme + " of entity " + ent); 
  }

  public void createUseCaseActivity(String ucname)
  { UseCase uc = (UseCase) ModelElement.lookupByName(ucname,useCases); 

    if (uc == null) { return; } 

    if (actDialog == null)
    { actDialog = new ActivityEditDialog(parent);
      actDialog.pack();
      actDialog.setLocationRelativeTo(this);
    }
    actDialog.setOldFields("","","","","",true);
    actDialog.setVisible(true);
   
    String post = actDialog.getPost(); 
    Compiler2 comp = new Compiler2(); 
    if (post == null)
    { System.out.println(">>>> ERROR: Invalid activity: " + post); 
      return; 
    }
    comp.nospacelexicalanalysis(post); 
    Statement effect = comp.parseStatement(entities,types); 

    while (effect == null)
    { System.out.println(">>>>> ERROR: Invalid activity code: " + post); 
      actDialog.setOldFields("","","","",post,true);
      actDialog.setVisible(true);
      post = actDialog.getPost(); 
      comp = new Compiler2(); 
      if (post == null)
      { System.out.println(">>>> ERROR: Invalid activity: " + post); 
        return; 
      }
      comp.nospacelexicalanalysis(post); 
      effect = comp.parseStatement(entities,types); 
    }

    // effect.setEntity(ent); 
    Vector contexts = new Vector(); 
    // contexts.add(ent); 
    // use case parameters
    Vector vars = new Vector(); 
    vars.addAll(uc.getParameters()); 

    effect.typeCheck(types,entities,contexts,vars); 
    Vector pres = effect.allPreTerms(); 
    System.out.println("All pre-expressions used: " + pres); 
    uc.setActivity(effect); 
    System.out.println("Set activity for use case " + ucname);

    updateActivities(uc,effect); 
    // remove any existing behaviour for uc!
  }

  public void editUseCaseActivity(String ucname)
  { UseCase uc = (UseCase) ModelElement.lookupByName(ucname,useCases); 

    if (uc == null) { return; } 

    Statement stat = uc.getActivity(); 

    if (actDialog == null)
    { actDialog = new ActivityEditDialog(parent);
      actDialog.pack();
      actDialog.setLocationRelativeTo(this);
    }
    actDialog.setOldFields("","","","","" + stat,true);
    actDialog.setVisible(true);
   
    String post = actDialog.getPost(); 
    Compiler2 comp = new Compiler2(); 
    if (post == null)
    { System.out.println(">>>>> ERROR: Invalid activity: " + post); 
      return; 
    }
    comp.nospacelexicalanalysis(post); 
    Statement effect = comp.parseStatement(entities,types); 

    while (effect == null)
    { System.out.println("ERROR: Invalid activity code: " + post); 
      actDialog.setOldFields("","","","",post,true);
      actDialog.setVisible(true);
      post = actDialog.getPost(); 
      comp = new Compiler2(); 
      if (post == null)
      { System.out.println(">>>>> ERROR: Invalid activity: " + post); 
        return; 
      }
      comp.nospacelexicalanalysis(post); 
      effect = comp.parseStatement(entities,types);       
    }
    // effect.setEntity(ent); 
    Vector contexts = new Vector(); 
    // contexts.add(ent); 
    Vector vars = new Vector(); 
    vars.addAll(uc.getParameters()); 

    effect.typeCheck(types,entities,contexts,vars); 
    Vector pres = effect.allPreTerms(); 
    System.out.println("All pre-expressions used: " + pres); 
    uc.setActivity(effect); 
    System.out.println("Set activity for use case " + ucname);

    updateActivities(uc,effect); 
  }

  private void updateActivities(Entity ent, BehaviouralFeature op, Statement effect)
  { // remove any existing activity for ent, op and replace by effect
    Vector removals = new Vector(); 
    for (int i = 0; i < activities.size(); i++) 
    { Behaviour act = (Behaviour) activities.get(i); 
      if (act.getEntity() == ent && act.specification == op)
      { removals.add(act); } 
    } 
    activities.removeAll(removals); 
    activities.add(new Behaviour(ent,op,effect)); 
  } 

  private void updateActivities(Entity ent, Statement effect)
  { // remove any existing activity for ent and replace by effect
    Vector removals = new Vector(); 
    for (int i = 0; i < activities.size(); i++) 
    { Behaviour act = (Behaviour) activities.get(i); 
      if (act.getEntity() == ent && act.specification == null)
      { removals.add(act); } 
    } 
    activities.removeAll(removals); 
    activities.add(new Behaviour(ent,effect)); 
  } 

  private void updateActivities(UseCase uc, Statement effect)
  { String ucname = uc.getName();  
    Vector removals = new Vector();  
    for (int i = 0; i < activities.size(); i++) 
    { Behaviour beh = (Behaviour) activities.get(i); 
      if (ucname.equals(beh.getName()))
      { removals.add(beh); } 
    } 
    activities.removeAll(removals); 
    activities.add(new Behaviour(uc,effect)); 
  } 

  private void removeAttributeFromEntity(Entity ent)
  { String nme = JOptionPane.showInputDialog("Enter name of attribute:");; 
    if (nme == null) { return; } 
    ent.removeAttribute(nme);
    JOptionPane.showMessageDialog(null, "Existing constraints may be invalidated!", "", JOptionPane.WARNING_MESSAGE);  
    resetDesigns(); 
    repaint(); 
  }


  private void moveSelected(int oldx, int oldy, int x, int y)
  { if (selectedVisual != null)
    { selectedVisual.changePosition(oldx,oldy,x,y); }
  } // and check/change underlying model

  public void moveAllRight()
  { for (int i = 0; i < visuals.size(); i++) 
    { VisualData vd = (VisualData) visuals.get(i); 
      vd.moveRight(150); 
    }
  } 

  public void moveAllDown()
  { for (int i = 0; i < visuals.size(); i++) 
    { VisualData vd = (VisualData) visuals.get(i); 
      vd.moveDown(150); 
    }
  } 

  public void moveAllLeft()
  { for (int i = 0; i < visuals.size(); i++) 
    { VisualData vd = (VisualData) visuals.get(i); 
      vd.moveLeft(150); 
    }
  } 

  public void moveAllUp()
  { for (int i = 0; i < visuals.size(); i++) 
    { VisualData vd = (VisualData) visuals.get(i); 
      vd.moveUp(150); 
    }
  } 

  public void resetSelected()
  { selectedVisual = null;
    selectedComponent = null;
    // selectedProcess = null; 
  }

  private LineData drawLineTo(RectForm vd1, RectForm vd2, int lineType) 
  { /* Assumes height of a RectForm always <= width */ 
    int startx;
    int starty;
    int endx;
    int endy;
    int cx1 = vd1.getx() + (vd1.width/2);
    int cy1 = vd1.gety() + (vd1.height/2);
    int cx2 = vd2.getx() + (vd2.width/2);
    int cy2 = vd2.gety() + (vd2.height/2);

    if (cx1 <= cx2)
    { startx = cx1 + (vd1.height/3);
      endx = cx2 - (vd2.height/3); }
    else 
    { startx = cx1 - (vd1.height/3);
      endx = cx2 + (vd2.height/3); }

    if (cy1 <= cy2)
    { starty = cy1 + (vd1.height/3);
      endy = cy2 - (vd2.height/3); }
    else 
    { starty = cy1 - (vd1.height/3);
      endy = cy2 + (vd2.height/3); }

    LineData ld = 
      new LineData(startx,starty, endx,endy, 
                   linecount,lineType);
    Flow fl = new Flow("f" + linecount,vd1,vd2); 
    linecount++; 
    ld.setLabel("");
    ld.setFlow(fl); 
    visuals.add(ld); 
    return ld; 
  }

  public void drawDependency(UseCase uc, Entity e, String kind)
  { RectForm rf1 = (RectForm) getVisualOf(uc); 
    RectForm rf2 = (RectForm) getVisualOf(e); 
    if (rf1 == null || rf2 == null) 
    { System.err.println("ERROR: Undefined visuals for " + uc + " " + e); 
      return; 
    } 
    LineData ld = drawLineTo(rf1, rf2, DASHED); 
    if (ld != null) 
    { ld.setLabel(kind); 
      ld.flow.setLabel(kind); 
    } 
    ld.setColour(Color.RED); 
    repaint();  
  } 

  public void drawDependency(Entity e, UseCase uc, String kind)
  { RectForm rf1 = (RectForm) getVisualOf(uc); 
    RectForm rf2 = (RectForm) getVisualOf(e); 
    if (rf1 == null || rf2 == null) 
    { System.err.println("ERROR: Undefined visuals for " + e + " " + uc); 
      return; 
    } 
    LineData ld = drawLineTo(rf2, rf1, DASHED); 
    if (ld != null) 
    { ld.setLabel(kind); 
      ld.flow.setLabel(kind); 
    } 
    ld.setColour(Color.GREEN); 
    repaint();  
  } 

  public void drawDependency(UseCase suc, UseCase tuc, String kind)
  { RectForm rf1 = (RectForm) getVisualOf(suc); 
    RectForm rf2 = (RectForm) getVisualOf(tuc); 
    if (rf1 == null || rf2 == null) 
    { System.err.println("ERROR: Undefined visuals for " + suc + " " + tuc); 
      return; 
    } 
    LineData ld = drawLineTo(rf1, rf2, DASHED); 
    if (ld != null) 
    { ld.setLabel(kind); 
      ld.flow.setLabel(kind);
      // ld.message = new Message(kind);  
    } 
    // ld.setColour(Color.GREEN); 
    repaint();  
  } 

  public void removeDependency(UseCase suc, UseCase tuc, String kind)
  { RectForm rf1 = (RectForm) getVisualOf(suc); 
    RectForm rf2 = (RectForm) getVisualOf(tuc); 
    if (rf1 == null || rf2 == null) 
    { System.err.println("ERROR: Undefined visuals for " + suc + " " + tuc); 
      return; 
    } 

    Vector deleted = new Vector(); 
    for (int i = 0; i < visuals.size(); i++) 
    { VisualData vd = (VisualData) visuals.get(i); 
      if (vd instanceof LineData) 
      { LineData ld = (LineData) vd;  
        if (ld.label.equals(kind) && 
            rf1.isUnder(ld.getx(), ld.gety()) && 
            rf2.isUnder(ld.getx2(), ld.gety2())) 
        { deleted.add(ld); }  
      } 
    } 
    visuals.removeAll(deleted); 

    repaint();  
  } 

  public void addType(String tname, Vector values)
  { Type x = (Type) ModelElement.lookupByName(tname,types); 
    if (x != null) 
    { System.err.println("ERROR: Redefining existing type -- not allowed!"); 
      return; 
    } 

    for (int i = 0; i < types.size(); i++) 
    { Type tt = (Type) types.get(i); 
      if (tt.valueClash(values))
      { System.err.println("ERROR: Duplicate value in different types: " + tt); 
        return; 
      } 
    } 

    Type t = new Type(tname,values);
    types.add(t); 
    if (values != null) // enumerated type
    { RectData rd = new RectData(10 + 40*types.size(),10,
                                 getForeground(),
                                 componentMode,
                                 rectcount);
      rectcount++;
      rd.setLabel(tname);
      rd.setModelElement(t); 
      visuals.add(rd); 
      repaint(); 
    } 
  } 


  public Type getType(String tname)
  { return (Type) ModelElement.lookupByName(tname,types); }

  public void listTypes()
  { System.out.println("********** Available types are: ************"); 
    for (int i = 0; i < types.size(); i++) 
    { Type tt = (Type) types.get(i); 
      System.out.println(tt.getName() + " = " + tt.getValues()); 
    } 
  } 

  public void listOperations(PrintWriter out)
  { out.println("************ Operations are: **************"); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      out.println("*** Operations of entity " + 
                         ent.getName() + ":");
      ent.listOperations(out);  
    } 
  } 

  private void addComponent(String nme,
                            int cType,
                            int x, int y)
  { /* Assume RSDS builder already set */
    RectData rd = 
      new RectData(x,y,getForeground(),
                   cType,rectcount);
    rectcount++;
    rd.setLabel(nme);
    visuals.add(rd); 
    componentNames.add(nme); 

    /* Statemachine sm =
      parent.createStatechartWindow(nme);
    if (cType == SENSOR)
    { sensors.add(sm); 
      sm.setcType(SENSOR); 
      if (processes.size() == 1)  // >= 1
      { drawLineTo(rd, (OvalData) processes.get(0), DASHED); }
    }
    else if (cType == ACTUATOR)
    { actuators.add(sm); 
      sm.setcType(ACTUATOR); 
      if (processes.size() == 1)  // >= 1
      { drawLineTo((OvalData) processes.get(0), rd, SOLID); }
    }
    rd.setComponent(sm);  */ 
  }

  public Entity reconstructEntity(String nme, int xx,
                                  int yy, String fname, String ecard,
                                  Vector stereotypes)
  { Entity e1 =
      (Entity) ModelElement.lookupByName(nme,entities);
    if (e1 != null)
    { e1.setStereotypes(stereotypes);  
      return e1;
    } 

    Entity ent = new Entity(nme); // not null or empty
    RectData rd =
      new RectData(xx,yy,getForeground(),componentMode,
                   rectcount);
    rectcount++;
    rd.setLabel(nme);
    entities.add(ent);
    visuals.add(rd);
    componentNames.add(nme);
    rd.setModelElement(ent);
    // restore attributes and rolenames from fname
    if (ecard == null || ecard.equals("null") || ecard.equals(""))
    { ent.setCardinality("*"); } 
    else 
    { ent.setCardinality(ecard); }
    ent.setStereotypes(stereotypes);  
    return ent;
  }

  public void addEntity(Entity ent, int xx, int yy) 
  { RectData rd =
      new RectData(xx,yy,getForeground(),componentMode,
                   rectcount);
    rectcount++;
    String nme = ent.getName(); 
    rd.setLabel(nme);

    if (entities.contains(ent)) { } 
    else 
    { entities.add(ent); } 

    visuals.add(rd);
    componentNames.add(nme);
    rd.setModelElement(ent);
  } 

  public void addEntity(Entity srcent, Entity trgent, int xx) 
  { VisualData vd = getVisualOf(srcent); 
    if (vd == null) { return; } 
    RectData rd =
      new RectData(xx + vd.getx(),vd.gety(),getForeground(),componentMode,
                   rectcount);
    rectcount++;
    String nme = trgent.getName(); 
    rd.setLabel(nme);
    entities.add(trgent);
    visuals.add(rd);
    componentNames.add(nme);
    rd.setModelElement(trgent);
  } 

  private void reconstructAssociation(String ename1,
                 String ename2, int xs, int ys, int xe,
                 int ye, int c1, int c2, String role2,
                 String role1, Vector stereotypes, Vector wpoints)
  { Entity e1 =
      (Entity) ModelElement.lookupByName(ename1,entities);
    Entity e2 =
      (Entity) ModelElement.lookupByName(ename2,entities);
    if (e1 == null || e2 == null)
    { System.out.println("Error in data, no entities " +
                         e1 + " " + e2);
      return;
    }
    if ("null".equals(role1))
    { role1 = null; }

    if (e1.hasRole(role2)) 
    { System.err.println("Entity " + e1 + " already has a role " + role2); 
      return; 
    } 
 
    Association ast =
      new Association(e1,e2,c1,c2,role1,role2);
    ast.setName("r" + associations.size()); 
    LineData sline = null; 
    Entity acent = null; 

    ast.setStereotypes(stereotypes); 
    if (stereotypes.contains("ordered"))
    { ast.setOrdered(true); } 
    if (stereotypes.contains("sorted"))
    { ast.setSorted(true); } 
    if (stereotypes.contains("addOnly"))
    { ast.setAddOnly(true); } 
    if (stereotypes.contains("readOnly"))
    { ast.setFrozen(true); } 
    if (stereotypes.contains("aggregation"))
    { ast.setAggregation(true); } 
    if (stereotypes.contains("qualifier"))
    { int qind = stereotypes.indexOf("qualifier"); 
      if (qind + 1 < stereotypes.size())
      { String qvar = (String) stereotypes.get(qind + 1); 
        Attribute qatt = new Attribute(qvar,new Type("String",null),ModelElement.INTERNAL); 
        qatt.setElementType(new Type("String",null)); 
        ast.unsetQualifier(); 
        ast.setQualifier(qatt);
      }   
    }          
    if (stereotypes.contains("associationClass"))
    { int acind = stereotypes.indexOf("associationClass"); 
      if (acind + 1 < stereotypes.size())
      { String acvar = (String) stereotypes.get(acind + 1); 
        acent = (Entity) ModelElement.lookupByName(acvar,entities); 
        ast.unsetLinkedClass(); 
        if (acent != null)
        { acent.setLinkedAssociation(ast); }   

        for (int i = 0; i < visuals.size(); i++)
        { VisualData vd = (VisualData) visuals.get(i); 
          ModelElement me = (ModelElement) vd.getModelElement(); 
          if (me == acent) // Entity
          { sline = 
              new ACLineData(xs,ys,xe,ye,linecount,SOLID, (RectData) vd);
          } 
        } 
      } 
      System.out.println("Reconstructed association class: " + acent + " " + sline); 
    }             

    associations.add(ast);
    e1.addAssociation(ast);
    if (role1 != null && role1.length() > 0)
    { Association invast = ast.generateInverseAssociation(); 
      invast.setCard2(c1); 
      e2.addAssociation(invast); 
      if (acent != null)
      { invast.setLinkedClass(acent); }   
    } 
        
    if (!stereotypes.contains("associationClass"))
    { sline =
        new LineData(xs,ys,xe,ye,linecount,SOLID);
    }

    linecount++;
    sline.setModelElement(ast);
    sline.setWaypoints(wpoints); 
    visuals.add(sline);
  }

  private void reconstructBehaviour(PreBehaviour pb)
  { Entity ent = (Entity) ModelElement.lookupByName(pb.behaviouredClassifier,entities); 
    UseCase uc = (UseCase) ModelElement.lookupByName(pb.behaviouredClassifier,useCases); 

    if (ent == null)
    { System.err.println("No entity " + pb.behaviouredClassifier);
      if (uc == null) 
      { System.out.println("No use case, either -- invalid activity"); 
        return;
      } 
    }

    Vector contexts = new Vector(); 
    if (ent != null) { contexts.add(ent); }  

    BehaviouralFeature op = null; 

    if (pb.specification == null || "null".equals(pb.specification))
    { op = null; } 
    else if (ent != null) 
    { op = ent.getOperation(pb.specification); } 

    if (pb.code == null)
    { return; } 
    Statement effect = pb.code; 

    if (ent != null) { effect.setEntity(ent); } 

    Vector vars = new Vector(); 
    if (op != null) 
    { vars.addAll(op.getParameters()); } 
 
    effect.typeCheck(types,entities,contexts,vars); 
    if (op != null)
    { op.setActivity(effect); 
      System.out.println("Set activity for operation " + pb.specification + 
                         " of entity " + ent); 
    }
    else if (ent != null) 
    { ent.setActivity(effect); 
      System.out.println("Set activity for entity " + ent); 
    }
    else if (uc != null) 
    { uc.setActivity(effect); 
      System.out.println("Set activity for use case " + uc.getName()); 
    } 


    Behaviour bb = new Behaviour(ent,op,effect); 
    activities.add(bb); 
  } 
  // use case operations can't have activities. 
    

  private void reconstructGeneralisation(PreGeneralisation pg)
  { Entity e1 =
      (Entity) ModelElement.lookupByName(pg.e1name,
                                         entities);  // superclass
   Entity e2 =
      (Entity) ModelElement.lookupByName(pg.e2name,
                                         entities);  // subclass
   if (e1 == null || e2 == null)
   { System.err.println("No entities " + e1 + " or " +
                        e2);
     return;
   }

   Generalisation g = new Generalisation(e1,e2);
   InheritLineData line =
     new InheritLineData(pg.xs,pg.ys,pg.xe,pg.ye,
                         linecount,SOLID);
   line.setModelElement(g);
   line.setWaypoints(pg.waypoints); 

   // System.out.println(e1.isInterface() + " " + e2.isInterface()); 

      if (e1.isInterface() || e2.getSuperclass() != null)
      { e2.addInterface(e1); 
        if (e1.selfImplementing())
        { System.err.println("Cycle of realizations: not allowed!"); 
          e2.removeInterface(e1); 
          return; 
        } 
        e1.addSubclass(e2);   // ?
        g.setRealization(true);
        generalisations.add(g);
      }
   /* if (e1.isInterface())
   { e2.addInterface(e1); 
     if (e1.selfImplementing())
     { System.err.println("Cycle of realizations: not allowed!"); 
       e2.removeInterface(e1); 
       return; 
     } // and remove e2 from e1
     e1.addSubclass(e2);   // ?
     g.setRealization(true);
     generalisations.add(g);
   }  */ 
   else 
   { e2.setSuperclass(e1); 
     e1.addSubclass(e2); 
     generalisations.add(g);
     boolean valid = formFamilies(g);
     if (!valid)
     { System.err.println("Invalid inheritance structure: " + g); 
       generalisations.remove(g); 
       e1.removeSubclass(e2); 
       e2.setSuperclass(null); 
       return; 
     }  
   }

   linecount++;
   visuals.add(line);
  }

  public void addInheritances(Entity e, Entity[] ents)
  { RectData rde = (RectData) getVisualOf(e); 
    if (rde == null) 
    { System.err.println("No visual for: " + e); 
      return; 
    } 

    for (int i = 0; i < ents.length; i++)
    { Entity ent = ents[i]; 
      RectData rdent = (RectData) getVisualOf(ent); 
      if (rdent == null) 
      { continue; } 
      Generalisation gen = new Generalisation(e, ent); 
      InheritLineData line =
        new InheritLineData(rdent.getx() + 5,rdent.gety(),
                            rde.getx() + 5,rde.gety() + rde.height - 10,
                            linecount,SOLID);
      line.setModelElement(gen);
      generalisations.add(gen);
      Entity oldsuper = ent.getSuperclass(); 

      if (oldsuper == null) 
      { ent.setSuperclass(e); } 
      else 
      { ent.addSuperclass(e); } 
 
      e.addSubclass(ent); 
      linecount++;
      visuals.add(line);
      formFamilies(gen);
    }
  }

  public void addInheritance(Generalisation g, Entity e, Entity ent)
  { RectData rde = (RectData) getVisualOf(e); 
    RectData rdent = (RectData) getVisualOf(ent); 
    if (rde == null || rdent == null) 
    { System.err.println("Missing visuals for " + e + " " + ent); 
      return; 
    }
 
    InheritLineData line =
        new InheritLineData(rdent.getx() + 5,rdent.gety(),
                            rde.getx() + 5,rde.gety() + rde.height - 10,
                            linecount,SOLID);
    line.setModelElement(g);
    generalisations.add(g);
    ent.setSuperclass(e); 
    e.addSubclass(ent); 
    linecount++;
    visuals.add(line);
    formFamilies(g);
  }

  public void addElements(Vector elems) 
  { for (int i = 0; i < elems.size(); i++) 
    { Object obj = elems.get(i); 
      if (obj instanceof Generalisation) 
      { Generalisation g = (Generalisation) obj; 
        Entity esup = g.getAncestor();
        Entity esub = g.getDescendent(); 
        RectData rsup = (RectData) getVisualOf(esup);  
        RectData rsub = (RectData) getVisualOf(esub); 
        if (rsup == null || rsub == null) 
        { continue; } 
        InheritLineData line =
          new InheritLineData(rsub.getx() + 10,rsub.gety(),
                            rsup.getx() + 10,rsup.gety() + rsup.height - 15,
                            linecount,SOLID);
        line.setModelElement(g);
        generalisations.add(g);
        esub.setSuperclass(esup); 
        esup.addSubclass(esub); 
        linecount++;
        visuals.add(line);
        formFamilies(g);
      }
      else if (obj instanceof Association) 
      { Association ast = (Association) obj; 
        Entity e1 = ast.getEntity1(); 
        Entity e2 = ast.getEntity2(); 
        RectData rd1 = (RectData) getVisualOf(e1);       
        RectData rd2 = (RectData) getVisualOf(e2);
        if (rd1 == null || rd2 == null) { continue; }  
        int x1 = rd1.getx(); 
        int y1 = rd1.gety(); 
        int x = rd2.getx(); 
        int y = rd2.gety(); 
        if (x1 <= x) { x1 = x1 + rd1.width - 5; } 
        else if (x < x1) { x = x + rd2.width - 5; } 
        if (y1 <= y) { y1 = y1 + rd1.height - 10; } 
        else if (y < y1) { y = y + rd2.height - 10; } 

        LineData sline = 
          new LineData(x1,y1,x,y,linecount,SOLID);
        // create Flow for it. 
        Flow flw = new Flow("f" + linecount); 
        flw.setSource(rd1); 
        flw.setTarget(rd2); 
        linecount++;
        sline.setFlow(flw);
        visuals.addElement(sline);
        associations.add(ast); 
        sline.setModelElement(ast);
      } 
    }
  } 


  public void addGeneralisations(Vector gens)
  { for (int i = 0; i < gens.size(); i++)
    { Generalisation g = (Generalisation) gens.get(i); 
      Entity esup = g.getAncestor();
      Entity esub = g.getDescendent(); 
      RectData rsup = (RectData) getVisualOf(esup);  
      RectData rsub = (RectData) getVisualOf(esub); 
      if (rsup == null || rsub == null) 
      { continue; } 
      InheritLineData line =
        new InheritLineData(rsub.getx() + 10,rsub.gety(),
                            rsup.getx() + 10,rsup.gety() + rsup.height - 15,
                            linecount,SOLID);
      line.setModelElement(g);
      generalisations.add(g);
      esub.setSuperclass(esup); 
      esup.addSubclass(esub); 
      linecount++;
      visuals.add(line);
      formFamilies(g);
    }
  }

  public void addAssociations(Vector asts)
  { for (int i = 0; i < asts.size(); i++)
    { Association ast = (Association) asts.get(i); 
      Entity e1 = ast.getEntity1(); 
      Entity e2 = ast.getEntity2(); 
      RectData rd1 = (RectData) getVisualOf(e1);       
      RectData rd2 = (RectData) getVisualOf(e2);
      if (rd1 == null || rd2 == null) { return; }  
      int x1 = rd1.getx(); 
      int y1 = rd1.gety(); 
      int x = rd2.getx(); 
      int y = rd2.gety(); 
      if (x1 <= x) { x1 = x1 + rd1.width - 5; } 
      else if (x < x1) { x = x + rd2.width - 5; } 
      if (y1 <= y) { y1 = y1 + rd1.height - 10; } 
      else if (y < y1) { y = y + rd2.height - 10; } 

      LineData sline = 
        new LineData(x1,y1,x,y,linecount,SOLID);
      // create Flow for it. 
      Flow flw = new Flow("f" + linecount); 
      flw.setSource(rd1); 
      flw.setTarget(rd2); 
      linecount++;
      sline.setFlow(flw);
      visuals.addElement(sline);
      associations.add(ast); 
      sline.setModelElement(ast);
    } 
  }

  public void removeAssociationClass(Association ast)
  { LineData ld = (LineData) getVisualOf(ast); 
    if (ld != null && ld instanceof ACLineData)
    { ((ACLineData) ld).myclass = null; } 
  }

  // same as addInvariant: 
  public Constraint addInvariant(PreConstraint pc)
  { if (pc.succ == null) 
    { System.err.println("Constraint not parsed correctly"); 
      return null;
    }
 
    Vector astv = new Vector();
    Entity owner = null; 
    UseCase usecase = null; 
    int constraintType = 1; 
    String preEntity = ""; 
    int atindex = -1; 

    Vector line4vals = pc.assocs; 
    for (int i = 0; i < line4vals.size(); i++)
    { String se = ((String) line4vals.get(i)).trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        // System.out.println("Pre-entity: " + se); 
        se = se.substring(0,atindex); 
      } 
      Association ast = 
        (Association) ModelElement.lookupByName(se,associations); 
      if (ast != null) 
      { // System.out.println("Found association: " + ast);
        astv.add(ast);
      } 
      else 
      { // System.out.println("Unknown association: " + se);
        Entity e = (Entity) ModelElement.lookupByName(se,entities); 
        if (e != null)
        { owner = e; } 
        else 
        { UseCase uc = (UseCase) ModelElement.lookupByName(se,useCases); 
          if (uc != null) 
          { // System.out.println("Found use case");
            usecase = uc;
          }
        }
      } 
    } 

    Vector contexts = new Vector(); 
    if (owner != null)
    { contexts.add(owner); } 
    else 
    { System.err.println("No owner specified: " + line4vals); } 

    Vector env = new Vector(); // parameters of use case if usecase != null
    if (usecase != null) 
    { env.addAll(usecase.getParameters()); 
      Type ucrt = usecase.getResultType(); 
      if (ucrt != null) 
      { Attribute att = new Attribute("result", ucrt, ModelElement.INTERNAL); 
        att.setElementType(usecase.getElementType()); 
        env.add(att); 
      }
    }  

    Constraint con = new Constraint(pc.cond0,pc.cond,pc.succ,astv);
    con.setOwner(owner); 

    if (pc.orderedBy != null) 
    { con.setOrdered(true); 
      con.setOrderedBy(pc.orderedBy); 
    } 

    if (preEntity.length() > 0) 
    { con.setisPre(true); 
      // System.out.println("Prestate owner"); 
    } 

    if (con.typeCheck(types,entities,contexts,env)) { } 
    else
    { System.err.println("ERROR: Constraint not correctly " +
                         "typed: " + con);
    }
    // System.out.println("Invariant type-checked correctly"); 
    con.setBehavioural(pc.succ.isUpdateable()); 
    

    if (usecase != null) 
    { usecase.addPostcondition(con); 
      con.setUseCase(usecase); 
    } 

      // constraints.add(cons); 
      
    boolean local = con.checkIfLocal(); 
    if (usecase != null) { } 
    else if (local)
    { owner.addInvariant(con); } 
    else  
    { constraints.add(con);   // global constraint not in a use case

        // Vector ents = cons.innermostEntities();
        // System.out.println("Needed entities are: " + ents);   
        // Get the innermostEntities of con. 
        // Make sure each is listed by itself, or is source or dest
        // of some association in assocs. 
      Vector baseEntities = con.innermostEntities();
      Vector endPoints = Association.getEndpoints(astv);
      Vector reachable = Association.reachableFrom(baseEntities,astv); 

      if (owner != null)
      { endPoints.add(owner); } 
      if (VectorUtil.subset(baseEntities,endPoints)) { }
      else
      { System.out.println("Warning, base entities: " + baseEntities +
                           " not subset of association ends: " + endPoints);
      }
      baseEntities.removeAll(reachable); 
      if (owner != null) 
      { Vector supers = owner.getAllSuperclasses(); 
        baseEntities.removeAll(supers);
      } 
      con.setNeeded(baseEntities); 
      /* System.out.println("Needed entities for " + con + 
                         " are " + baseEntities);  */ 
      for (int y = 0; y < baseEntities.size(); y++) 
      { Entity ey = (Entity) baseEntities.get(y); 
        Association newa = findSubclassAssociation(ey,astv); 
        if (newa != null) 
        { if (astv.contains(newa)) { } 
          else 
          { astv.add(newa); } 
          // System.out.println("Found subclass assoc: " + newa); 
        } // remove the ancestor assoc.
        else 
        { newa = findSubclass2Association(ey,astv); 
          if (newa != null) 
          { if (astv.contains(newa)) { }
            else 
            { astv.add(newa); }
            // System.out.println("Found subclass assoc: " + newa); 
          }
        }
      } 
      con.setAssociations(astv);  
    }
    return con; 
  } 

  public Constraint addAssertion(PreConstraint pc)
  { if (pc.succ == null) 
    { System.err.println("Constraint not parsed correctly"); 
      return null;
    }
 
    Vector astv = new Vector();
    Entity owner = null; 
    UseCase usecase = null; 
    int constraintType = 1; 
    int atindex = -1; 
    String preEntity = ""; 

    Vector line4vals = pc.assocs; 
    for (int i = 0; i < line4vals.size(); i++)
    { String se = ((String) line4vals.get(i)).trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        se = se.substring(0,atindex); 
      } 
      Association ast = 
        (Association) ModelElement.lookupByName(se,associations); 
      if (ast != null) 
      { // System.out.println("Found association: " + ast);
        astv.add(ast);
      } 
      else 
      { // System.out.println("Unknown association: " + se);
        Entity e = (Entity) ModelElement.lookupByName(se,entities); 
        if (e != null)
        { owner = e; } 
        else 
        { UseCase uc = (UseCase) ModelElement.lookupByName(se,useCases); 
          if (uc != null) 
          { // System.out.println("Found use case");
            usecase = uc;
          }
        }
      } 
    } 

    Vector contexts = new Vector(); 
    if (owner != null)
    { contexts.add(owner); } 

    Constraint con = new Constraint(pc.cond0,pc.cond,pc.succ,astv);
    con.setOwner(owner); 

    if (preEntity.length() > 0)
    { con.setisPre(true); }  // shouldn't occur for precondition!

    Vector env = new Vector(); // parameters of use case if usecase != null
    if (usecase != null) 
    { env.addAll(usecase.getParameters()); 
      Type ucrt = usecase.getResultType(); 
      if (ucrt != null) 
      { Attribute att = new Attribute("result", ucrt, ModelElement.INTERNAL); 
        att.setElementType(usecase.getElementType()); 
        env.add(att); 
      }
    }  

    if (con.typeCheck(types,entities,contexts,env)) { } 
    else
    { System.out.println("ERROR: Constraint not correctly " +
                         "typed: " + con);
      // return null; 
    }
    // System.out.println("Invariant type-checked correctly"); 
    con.setBehavioural(pc.succ.isUpdateable()); 
    
    // for (int i = 0; i < astv.size(); i++) 
    // { Association ast = (Association) astv.get(i); 
    //    ast.addConstraint(cons); 
    //  } 
    //  cons.setOwner(owner); 
    //  cons.typeCheck(types,entities); // to identify variables
    // invariants.add(inv); 

    Vector lvars = new Vector(); 
    Vector qvars = con.secondaryVariables(lvars,env);  

    if (usecase != null) 
    { usecase.addPrecondition(con); 
      con.setUseCase(usecase); 
    } 

      // constraints.add(cons); 
      
    return con; 
  } 

  public Constraint addUCInvariant(PreConstraint pc)
  { if (pc.succ == null) 
    { System.err.println("Constraint not parsed correctly"); 
      return null;
    }
 
    Vector astv = new Vector();
    Entity owner = null; 
    UseCase usecase = null; 
    int constraintType = 1; 
    int atindex = -1; 
    String preEntity = ""; 

    Vector line4vals = pc.assocs; 
    for (int i = 0; i < line4vals.size(); i++)
    { String se = ((String) line4vals.get(i)).trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        se = se.substring(0,atindex); 
      } 
      Association ast = 
        (Association) ModelElement.lookupByName(se,associations); 
      if (ast != null) 
      { // System.out.println("Found association: " + ast);
        astv.add(ast);
      } 
      else 
      { // System.out.println("Unknown association: " + se);
        Entity e = (Entity) ModelElement.lookupByName(se,entities); 
        if (e != null)
        { owner = e; } 
        else 
        { UseCase uc = (UseCase) ModelElement.lookupByName(se,useCases); 
          if (uc != null) 
          { // System.out.println("Found use case");
            usecase = uc;
          }
        }
      } 
    } 

    Vector contexts = new Vector(); 
    if (owner != null)
    { contexts.add(owner); } 

    Constraint con = new Constraint(pc.cond0,pc.cond,pc.succ,astv);
    con.setOwner(owner); 
    if (preEntity.length() > 0) 
    { con.setisPre(true); } 

    Vector env = new Vector(); // parameters of use case if usecase != null
    if (usecase != null) 
    { env.addAll(usecase.getParameters()); 
      Type ucrt = usecase.getResultType(); 
      if (ucrt != null) 
      { Attribute att = new Attribute("result", ucrt, ModelElement.INTERNAL); 
        att.setElementType(usecase.getElementType()); 
        env.add(att); 
      }
    }  

    if (con.typeCheck(types,entities,contexts,env)) { } 
    else
    { System.out.println("Constraint not correctly " +
                         "typed: " + con);
      // return null; 
    }
    // System.out.println("Invariant type-checked correctly"); 
    con.setBehavioural(pc.succ.isUpdateable()); 
    
    // for (int i = 0; i < astv.size(); i++) 
    // { Association ast = (Association) astv.get(i); 
    //    ast.addConstraint(cons); 
    //  } 
    //  cons.setOwner(owner); 
    //  cons.typeCheck(types,entities); // to identify variables
    // invariants.add(inv); 

    Vector lvars = new Vector(); 
    Vector qvars = con.secondaryVariables(lvars,env);  

    if (usecase != null) 
    { usecase.addInvariant(con); 
      con.setUseCase(usecase); 
    } 
      
    return con; 
  } 


  public Constraint addGenericAssertion(PreConstraint pc, Vector ucs)
  { if (pc.succ == null) 
    { System.err.println("Constraint not parsed correctly"); 
      return null;
    }
 
    Vector astv = new Vector();
    Entity owner = null; 
    UseCase usecase = null; 
    int constraintType = 1; 
    int atindex = -1; 
    String preEntity = ""; 

    Vector line4vals = pc.assocs; 
    for (int i = 0; i < line4vals.size(); i++)
    { String se = ((String) line4vals.get(i)).trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        se = se.substring(0,atindex); 
      } 
      UseCase uc = (UseCase) ModelElement.lookupByName(se,ucs); 
      if (uc != null) 
      { // System.out.println("Found use case");
        usecase = uc;
      } 
      else 
      { System.out.println("Enter instantiation of entity: " + se); 
        String epar = 
            JOptionPane.showInputDialog("Actual value of " + se + ":");
        Entity e = (Entity) ModelElement.lookupByName(epar,entities); 
        if (e == null) 
        { System.err.println("Error: not valid entity " + epar); } 
        else
        { owner = e; } 
      } 
    } 

    Constraint con = new Constraint(pc.cond0,pc.cond,pc.succ,astv);
    con.setBehavioural(pc.succ.isUpdateable()); 
    con.setOwner(owner); 
    if (preEntity.length() > 0) 
    { con.setisPre(true); } // should not occur for a precondition

    // Vector lvars = new Vector(); 
    // Vector qvars = con.secondaryVariables(lvars);  

    if (usecase != null) 
    { usecase.addPrecondition(con); 
      con.setUseCase(usecase); 
    } 
      
    return con; 
  } 

  /* Actually generic postcondition: */ 

  public Constraint addGenericInvariant(PreConstraint pc, Vector ucs)
  { if (pc.succ == null) 
    { System.err.println("Constraint not parsed correctly"); 
      return null;
    }
 
    Vector astv = new Vector();
    Entity owner = null; 
    UseCase usecase = null; 
    int constraintType = 1; 
    String preEntity = ""; 
    int atindex = -1; 

    Vector line4vals = pc.assocs; 
    for (int i = 0; i < line4vals.size(); i++)
    { String se = ((String) line4vals.get(i)).trim();
      atindex = se.indexOf('@'); 
      if (atindex > 0)
      { preEntity = se; 
        se = se.substring(0,atindex); 
      } 
      UseCase uc = (UseCase) ModelElement.lookupByName(se,ucs); 
      if (uc != null) 
      { // System.out.println("Found use case");
        usecase = uc;
      }
      else 
      { System.out.println("Enter instantiation of entity: " + se);
        String epar = 
            JOptionPane.showInputDialog("Actual entity of: " + se);
        Entity e = (Entity) ModelElement.lookupByName(epar,entities); 
        if (e != null)
        { owner = e; } 
        else 
        { System.err.println("Not valid defined entity " + epar); }    
      } 
    } 

    Vector contexts = new Vector(); 
    if (owner != null)
    { contexts.add(owner); } 
    else 
    { System.err.println("ERROR: No owner specified"); } 

    Constraint con = new Constraint(pc.cond0,pc.cond,pc.succ,astv);
    con.setBehavioural(pc.succ.isUpdateable()); 
    con.setOwner(owner); 
    if (preEntity.length() > 0) 
    { con.setisPre(true); }     

    if (usecase != null) 
    { usecase.addPostcondition(con); 
      con.setUseCase(usecase); 
    } 
 
    return con; 
  } 

  public Constraint reconstructConstraint(PreConstraint pc)
  { Vector rels = new Vector();
    Entity ent = null; 

    Vector line4vals = pc.assocs; 

    for (int j = 0; j < line4vals.size(); j++)
    { String rname = (String) line4vals.get(j);
      Association ast =
        (Association) ModelElement.lookupByName(rname,
                                                associations);
      if (ast == null)
      { System.out.println("ERROR: Unknown association: " +
                           rname);
        ent = (Entity) ModelElement.lookupByName(rname,entities); 
      }
      else
      { rels.add(ast); }
    }

    Constraint con = new Constraint(pc.cond0,pc.cond,pc.succ,rels);
    con.setBehavioural(pc.succ.isUpdateable()); 
    con.setOwner(ent); 

    if (pc.orderedBy != null) 
    { con.setOrdered(true); 
      con.setOrderedBy(pc.orderedBy); 
    } 

    Vector contexts = new Vector(); 
    if (ent != null) 
    { contexts.add(ent); } 

    if (con.typeCheck(types,entities,contexts)) { } 
    else
    { System.out.println("Constraint not correctly " +
                         "typed: " + con);
      // return null; 
    }
    // Expression ante = Expression.simplify("&",pc.cond0,pc.cond); 
    // invariants.add(new SafetyInvariant(ante,pc.succ)); 

    boolean local = con.checkIfLocal(); 
    if (local) 
    { ent.addInvariant(con); } 
    else 
    { constraints.add(con);  // NOT CORRECT:  
      // Vector feats = con.allFeaturesUsedIn();
      // Vector assocs = relatedAssociations(feats); // only checks role names
      for (int j = 0; j < rels.size(); j++)
      { Association aa = (Association) rels.get(j);
        aa.addConstraint(con); 
      } 
      //  if (rels.contains(aa)) { }
      //  else
      //   { System.out.println("Association " + aa + " missing from constraint");
      //    con.addAssociation(aa);
      //  }
      // }
      Vector baseEntities = con.innermostEntities();
      Vector endPoints = Association.getEndpoints(rels);
      Vector reachable = Association.reachableFrom(baseEntities,rels); 

      endPoints.add(ent); 
      if (VectorUtil.subset(baseEntities,endPoints)) { }
      else
      { System.out.println("Warning, base entities: " + baseEntities +
                           " not subset of association ends: " + endPoints);
      }
      baseEntities.removeAll(reachable); 
      con.setNeeded(baseEntities);
      System.out.println("Needed entities of " + con + " are: " + baseEntities); 
    }

    return con; 
  } 

  private BehaviouralFeature reconstructOperation(PreOp p)
  { // look up the entity or use case. Should allow multiple post and pre's
    
    String pucn = p.entname.toLowerCase();
    Entity ent = null; 
    UseCase uc = null; 

    if ("null".equals(p.entname))  // Operation of a use case or global op
    { if (p.ucname == null) { } 
      else 
      { uc = (UseCase) ModelElement.lookupByName(p.ucname,useCases); } 
    } 
 
    if (uc == null) // normal case of an entity operation
    { ent = (Entity) ModelElement.lookupByName(p.entname,entities); } 

    boolean query = true; 
    String typ = p.resultType;     

    Type elemType = null; 
    Type tt = null; 

    if (typ != null) 
    { tt = Type.getTypeFor(typ,types,entities); } 


    String params = p.params; // pairs var type
    Vector oppars = 
      BehaviouralFeature.reconstructParameters(params,types,entities);
    String pre = p.pre; 
    String post = p.post; 
   
    Compiler2 comp = new Compiler2(); 
    comp.nospacelexicalanalysis(pre);
    Expression cond = comp.parse();
    if (cond == null || pre == null || pre.equals("null"))
    { cond = new BasicExpression(true); } 

    Vector contexts = new Vector(); 
    if (ent != null) 
    { contexts.add(ent); } 

    Vector vars = new Vector(); 
    vars.addAll(oppars);
    Attribute resultvar = null;  

    if (tt != null)
    { resultvar = new Attribute("result",tt,ModelElement.INTERNAL); 
      resultvar.setElementType(tt.getElementType()); 
      vars.add(resultvar); 
      elemType = tt.getElementType(); 
    } 

    boolean tc = cond.typeCheck(types,entities,contexts,vars);
    if (!tc) 
    { System.err.println("Invalid precondition: " + cond); 
      // return null; 
      JOptionPane.showMessageDialog(null, "ERROR: Invalid precondition " + cond + " for: " + p.name,
                                    "Expression error", JOptionPane.ERROR_MESSAGE); 
    }

    Compiler2 comp1 = new Compiler2(); 
    comp1.nospacelexicalanalysis(post); 
    Expression effect = comp1.parse(); 
    if (post == null || post.equals("null"))
    { System.err.println("ERROR: Invalid postcondition " + post); 
      effect = null; 
    } 

    BehaviouralFeature op = new BehaviouralFeature(p.name,oppars,query,tt);
    if (tt != null)
    { op.setElementType(elemType); } 
    if (resultvar != null) 
    { resultvar.setElementType(op.getElementType()); } 

    System.out.println(">>> Reconstructed " + op.name + " of " + ent + " use case " + uc); 
    
    if (ent != null)
    { ent.addOperation(op); } // else add to global/controller ops. Stereotypes?
    // op.setEntity(ent); 
    else if (uc != null) 
    { uc.addOperation(op); 
      op.setStatic(true); 
      op.setUseCase(uc);  
    } // must be static in this case
    
    StringTokenizer st = new StringTokenizer(p.stereotypes," "); 
    Vector strs = new Vector(); 
    while (st.hasMoreTokens())
    { String pp = st.nextToken();
      strs.add(pp); 
      op.addStereotype(pp);
    } 

    if (strs.contains("query")) { } 
    else 
    { op.setQuery(false); } 

    if (strs.contains("static"))
    { op.setInstanceScope(false); } 
 
    if (strs.contains("cached"))
    { op.setCached(true); } 

    // sorted value, also? 

    if (effect != null) 
    { boolean tc2 = effect.typeCheck(types,entities,contexts,vars); 
      if (!tc2) 
      { System.err.println("Warning: unable to type postcondition: " + effect); 
        // return null; 
      }
    }
   
    op.setPre(cond); 
    op.setPost(effect); 

    return op; 
  }

  public void formShortestPaths()
  { AssociationPaths paths = new AssociationPaths(entities); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      paths.addNewAssociation(ast.getEntity1(),ast.getEntity2(),ast); 
      System.out.println("Shortest paths: " + paths); 
    }
  } 

  public void setComponentMode(int mode) 
  { componentMode = mode; } 

  public void setComponentName(String name) 
  { componentName = name; } 

  public void find_src_targ(LineData line, Flow flw)
  { for (int i = 0; i < visuals.size(); i++)
    { if (visuals.get(i) instanceof RectData)
      { RectData rd = (RectData) visuals.get(i);
        if (rd.isUnder(line.xstart,line.ystart))
        { flw.setSource(rd); 
          System.out.println(" source ==> " + rd.label);
        }
        if (rd.isUnder(line.xend, line.yend))
        { flw.setTarget(rd); 
          System.out.println(" target ==> " + rd.label); 
        }
      }
    }
  }

  private Association defineAssociation(Flow flw, Entity ent)
  { RectData src = (RectData) flw.getSource();
    RectData trg = (RectData) flw.getTarget();
    if (src == null || trg == null)
    { System.out.println("Line start or end not over a class");
      return null;
    }
    ModelElement me1 = src.getModelElement();
    ModelElement me2 = trg.getModelElement();
    if (me1 instanceof Entity && me2 instanceof Entity) 
    { Entity ent1 = (Entity) me1; 
      Entity ent2 = (Entity) me2; 
      if (ent1.isInterface())
      { System.err.println("Warning: defining association from interface");
        // return null; 
      }
      // Prompt user for target role name and both cards
      if (astDialog == null)
      { astDialog = new AstEditDialog(parent); 
        astDialog.pack();
        astDialog.setLocationRelativeTo(this);
      }

      String astname = "r" + associations.size(); 
      astDialog.setOldFields(astname,"","","","",false,false,false);
      // ent1.getName() + "_" + ent2.getName(),
      astDialog.setVisible(true);
     
      String role2 = astDialog.getRole2(); 
      if (role2 == null)
      { System.out.println("Add cancelled");
        return null;
      }
      String card1 = astDialog.getCard1();
      String card2 = astDialog.getCard2(); 
      int c1 = ModelElement.convertCard(card1);
      int c2 = ModelElement.convertCard(card2);
      String role1 = astDialog.getRole1(); 
      if ("".equals(role1))
      { role1 = null; } 

      String nme = astDialog.getName(); 
      { Association def0 = (Association) ModelElement.lookupByName(nme,associations);
        if (def0 != null)
        { System.err.println("ERROR: Association with this name already exists!"); 
          return null; 
        }
      }

      if (ent1.hasRole(role2))
      { System.err.println("ERROR: " + ent1 + " already has role with name " + role2 + "!"); 
        return null; 
      } 

       
      Association ast = 
        new Association(ent1,ent2,c1,c2,role1,role2);
      if (c1 == ModelElement.AGGREGATION1)
      { ast.setCard1(ModelElement.ONE); 
        ast.setAggregation(true); 
        System.out.println("Creating aggregation"); 
      }
      else if (c1 == ModelElement.AGGREGATION01)
      { ast.setCard1(ModelElement.ZEROONE); 
        ast.setAggregation(true); 
        System.out.println("Creating aggregation"); 
      }
      else if (c1 == ModelElement.QUALIFIER)
      { ast.setCard1(ModelElement.MANY); // assume
        String qualatt = 
          JOptionPane.showInputDialog("Enter qualifier name:");
        Attribute qatt = new Attribute(qualatt,new Type("String",null),
                                       ModelElement.INTERNAL); 
        qatt.setElementType(new Type("String", null)); 
        ast.setQualifier(qatt); 
      }         
      ast.setOrdered(astDialog.getOrdered());
      ast.setFrozen(astDialog.getFrozen()); 
      ast.setAddOnly(astDialog.getAddOnly()); 
      ast.setName(nme);  
      String stereotypes = astDialog.getStereotypes(); 
      if (stereotypes != null && !(stereotypes.equals("none")))
      { ast.addStereotype(stereotypes); } 

      // add it to the source entity
      ent1.addAssociation(ast);
      if (role1 != null && role1.length() > 0)
      { Association invast = ast.generateInverseAssociation(); 
        ent2.addAssociation(invast); 
        if (ent != null) // association class
        { invast.setLinkedClass(ent); } 
      } 
      return ast;
    } 
    else 
    { System.out.println("Association must be drawn between classes"); 
      return null; 
    } 
  }  // must use these in drawing the line.

  private Generalisation defineGeneralisation(Flow flw)
  { RectData src = (RectData) flw.getSource();
    RectData trg = (RectData) flw.getTarget();
    if (src == null || trg == null) 
    { System.out.println("ERROR: Line start or end not over a class"); 
      return null; 
    } 

    ModelElement me1 = src.getModelElement();
    ModelElement me2 = trg.getModelElement();

    if (me1 instanceof Entity && me2 instanceof Entity &&
        me1 != me2)
    { Entity e1 = (Entity) me1;
      Entity e2 = (Entity) me2;
      if (e2.isLeaf())
      { System.err.println("ERROR: Cannot have subclass of leaf class!"); 
        return null; 
      }

      if (e1.isInterface() && !e2.isInterface())
      { System.err.println("ERROR: Cannot have subinterface of class!"); 
        return null; 
      }

      if (e2.isAbstract() || e2.isInterface()) { } 
      else 
      { System.err.println("Warning: Superclass should be abstract!"); } 

      if (e1.getSuperclass() != null)
      { System.err.println("Warning: Multiple inheritance not permitted in Java/C#/C!"); 
        // return null; 
      } 
       
      Generalisation g = new Generalisation(e2,e1);
      String nme = g.getName(); 
      { Generalisation def0 =
          (Generalisation) ModelElement.lookupByName(nme,generalisations);
        if (def0 != null)
        { System.err.println("ERROR: Inheritance with this name already exists!"); 
          return null; 
        }
      }

      if (e2.isInterface() || e1.getSuperclass() != null)
      { e1.addInterface(e2); 
        if (e2.selfImplementing())
        { System.err.println("ERROR: Cycle of realizations: not allowed!"); 
          e1.removeInterface(e2); 
          return null; 
        } // and remove e2 from e1
        e2.addSubclass(e1);   // ?
        g.setRealization(true);
      }
      else
      { e1.setSuperclass(e2); 
        e2.addSubclass(e1); 
        boolean valid = formFamilies(g); 
        if (!valid) 
        { System.err.println("ERROR: Invalid inheritance structure"); 
          e2.removeSubclass(e1); 
          e1.setSuperclass(null); 
          return null; 
        } 
      }
      return g;
    }
    System.out.println("ERROR: Generalisation must be drawn " +
                       "between different classes");
    return null;
  }

  public void clearAllData()
  { entities = new Vector();
    associations = new Vector(); 
    visuals = new Vector(); 
    types = new Vector(); 
    generalisations = new Vector(); 
    constraints = new Vector(); 
    invariants = new Vector(); 
    componentNames = new Vector(); 
    useCases = new Vector(); 
  } 

  public void setDrawMode(int mode) 
  { switch (mode) 
    { case SLINES:
      case DLINES:
      case OVAL:
      case EDIT: 
      case ACLINES: 
        this.mode = mode;
        break;
      case POINTS:
        this.mode = mode;
        break;
      case EVENTS:
        //if (nameDialog == null)
        //{ // Create the new dialog box
          // nameDialog = new EvtNameDialog(parent); 
          // nameDialog.pack();
		    //set the location and make it visible
          // nameDialog.setLocationRelativeTo(parent);
        //}
	    // Make dialogue box visible (already created)
	    // nameDialog.setVisible(true); 
       // nameDialog.show(); 
	    
	    // Get event name entered from the textfield 
      // String txt = nameDialog.getValidatedText();
      // if (txt != null) 
      // { System.out.println("The event entered is valid.");
      //  eventlist.addElement(txt); 
      // }
	   break;
    case INERT: 
      this.mode = mode; 
      break; 
    default:
      throw new IllegalArgumentException();
    }
  }
      
  public void mouseClicked(MouseEvent me)
  { requestFocus(); } 
    
    public void mouseEntered(MouseEvent me)
    { /* System.out.println("Mouse entered"); */ } 
    
    public void mouseExited(MouseEvent me)
    { /* System.out.println("Mouse exited"); */ } 

    // This procedure returns true if element 
    // drawn is larger than the 
    // view and the scrollbars have to be adjusted.
    public boolean changed(int x, int y, int W, int H)
    { boolean change = false; 

      int this_width = (x + W + 10);
      if (this_width > preferredSize.width)
      { preferredSize.width = this_width;
        change = true;
      }
	
      int this_height = (y + H + 10);
      if (this_height > preferredSize.height)
      { preferredSize.height = this_height; 
        change = true;
      }
      return change;
    }

  public void keyPressed(KeyEvent e)
  { requestFocus();
    if (firstpress) 
    { System.out.println("Adding waypoint at " + x2 + " " + y2); 
      waypoints.add(new LinePoint(x2,y2)); 
    } 
    System.out.println(e);
    repaint(); 
  }

  public void keyReleased(KeyEvent e) 
  { } 

  public void keyTyped(KeyEvent e) 
  { } 
  

        
  public void mousePressed(MouseEvent me)
  { int x = me.getX(); 
    int y = me.getY(); 
    boolean is_bigger = false;
    System.out.println("Mouse pressed at " + 
                       x + " " + y); 

    requestFocus();

    switch (mode) 
    { case SLINES:
      case ACLINES: 
        System.out.println("Drag and release to draw association");
        x1 = x;
        y1 = y;    // Start line  
        firstpress = true;	
        waypoints.clear(); 
        break;
      case DLINES:
        System.out.println("Drag and release to draw inheritance");
        x1 = x;
        y1 = y;    // Start line  
        firstpress = true;	
        waypoints.clear(); 
        break;
      case POINTS: // for classes
        System.out.println("Creating a class");
        is_bigger = changed(x,y,50,50);
        RectData rd = new RectData(x,y,
                            getForeground(),
                            componentMode,
                            rectcount);
        rectcount++;
        Entity ent = null;
        if (componentName != null) 
        { rd.setLabel(componentName);
          // check not already defined: 
          Entity ee = (Entity) ModelElement.lookupByName(componentName,entities); 
          if (ee != null) 
          { System.err.println("ERROR: Entity with name already exists!"); 
            return; 
          } 
          ent = new Entity(componentName);
          // open edit dialog for entity
          if (entDialog == null)
          { entDialog = new EntityCreateDialog(parent); 
            entDialog.pack();
            entDialog.setLocationRelativeTo(this);
          }
          entDialog.setOldFields(componentName,"*","");
          entDialog.setVisible(true);
     
          String ecard = entDialog.getCard();
          ent.setCardinality(ecard);  // could be null 
          String stereo = entDialog.getStereotypes(); 
          if (stereo != null && !stereo.equals("none"))
          { ent.addStereotype(stereo);
            if (stereo.equals("active"))
            { ent.addOperation(new BehaviouralFeature("run", new Vector(), 
                                                      false, null)); 
            }
          } 
          entities.add(ent); 
          componentName = null; 
        } 
        visuals.addElement(rd); 
        componentNames.addElement(rd.label); 
        rd.setModelElement(ent);  // why not? 
        x1 = x;
        y1 = y;
        if (is_bigger)
        { // Update client's preferred size because 
          // the area taken up by the graphics has
          // gotten larger or smaller (got cleared).
          setPreferredSize(preferredSize);

          // Let the scroll pane know to update itself
          // and its scrollbars.
          revalidate();  		  
        }
        repaint();
        mode = INERT;
        break;
      case OVAL:
        System.out.println("This is OVAL");
        mode = INERT; 
        break;
      case EDIT:
        findSelected(x,y); 

        if (editMode == DELETING)
        { deleteSelected();
          resetSelected(); 
          setAppropriateCursor(INERT); 
        }  /* else, its MOVING or MODIFY */ 
        else if (editMode == MODIFY) 
        { modifySelected(); 
          setAppropriateCursor(INERT);
        } 
        repaint(); 
        break;
      default:  /* Including INERT */ 
        // if (me.isPopupTrigger())
        { findSelected(x,y); 
          modifySelected();
        }  
        // System.out.println("This is default");
        break;
    }
  }

  public void mouseReleased(MouseEvent e)
  { int x = e.getX();
    int y = e.getY();
    System.out.println("Mouse released at " + x + " " + y); 
    switch (mode) {
    case SLINES:  
      LineData sline = 
        new LineData(x1,y1,x,y,linecount,SOLID);
      if (sline.LineLength() < 5) 
      { System.err.println("ERROR: line too short!"); 
        firstpress = false; 
        mode = INERT; 
        return; 
      } 
      // create Flow for it. 
      Flow flw = new Flow("f" + linecount); 
      find_src_targ(sline,flw); 
      Association ast = defineAssociation(flw,null);
      if (ast != null) 
      { linecount++;
        sline.setFlow(flw);
        visuals.addElement(sline);
        associations.add(ast); 
        sline.setModelElement(ast);
        sline.setWaypoints((Vector) ((Vector) waypoints).clone()); 
      } 
      firstpress = false;
      mode = INERT; 
      repaint(); 
      break;
    case ACLINES: 
      int midx = (x1 + x)/2; 
      int midy = (y1 + y)/2; 
      RectData rd = new RectData(midx,midy+25,
                            getForeground(),
                            componentMode,
                            rectcount);
      rectcount++;
      Entity ent = null;
      if (componentName != null) 
      { rd.setLabel(componentName);
        // check not already defined: 
        Entity ee = (Entity) ModelElement.lookupByName(componentName,entities); 
        if (ee != null) 
        { System.err.println("ERROR: Entity with name already exists!"); 
          return; 
        } 
        ent = new Entity(componentName);
        // open edit dialog for entity
        if (entDialog == null)
        { entDialog = new EntityCreateDialog(parent); 
          entDialog.pack();
          entDialog.setLocationRelativeTo(this);
        }
        entDialog.setOldFields(componentName,"*","");
        entDialog.setVisible(true);
     
        String ecard = entDialog.getCard();
        ent.setCardinality(ecard);  // could be null 
        String stereo = entDialog.getStereotypes(); 
        if (stereo != null && !stereo.equals("none"))
        { ent.addStereotype(stereo); 
          if (stereo.equals("active"))
          { ent.addOperation(new BehaviouralFeature("run",new Vector(), false, 
                                                    null)); 
          }
        } 
        entities.add(ent); 
         
        visuals.addElement(rd); 
        componentNames.addElement(rd.label); 
        rd.setModelElement(ent);  // why not?
        ACLineData acline = 
          new ACLineData(x1,y1,x,y,linecount,SOLID,rd);
      // create Flow for it. 
        Flow flw4 = new Flow("f" + linecount); 
        find_src_targ(acline,flw4); 
        Association astcls = defineAssociation(flw4,ent);
        if (astcls != null) 
        { linecount++;
          acline.setFlow(flw4);
          astcls.setName(componentName); 
          visuals.addElement(acline);
          associations.add(astcls); 
          acline.setModelElement(astcls);
          ent.setLinkedAssociation(astcls); 
        } 
        firstpress = false;
     }  
        x1 = x;
        y1 = y;
        repaint();
        mode = INERT;
      break; 
    case DLINES:   // for inheritances
      LineData dline = 
        new InheritLineData(x1,y1,x,y,linecount,SOLID);  // dashed for realizations
      Flow flw2 = new Flow("f" + linecount); 
      find_src_targ(dline,flw2); 
      Generalisation gen = defineGeneralisation(flw2); 
      if (gen != null) 
      { linecount++;
        dline.setFlow(flw2); 
        visuals.addElement(dline);
        generalisations.add(gen); 
        dline.setModelElement(gen); 
        dline.setWaypoints((Vector) ((Vector) waypoints).clone()); 
      } 
      firstpress = false;
      mode = INERT; 
      repaint(); 
      break;
    case POINTS:
      break;
    case OVAL:
      break;
    case EDIT: 
      mode = INERT; 
      resetSelected();
      setAppropriateCursor(INERT);
      break; 
    default:
      break;
    }
    repaint(); 
  } 
  
  public void mouseDragged(MouseEvent e)
  { /* System.out.println("Mouse dragged");  */ 
    int x = e.getX();
    int y = e.getY();
    switch (mode) {
    case ACLINES:
    case SLINES:  
    case DLINES:  
      prevx = x2;
      prevy = y2;
      x2 = x;
      y2 = y;	
      break;
    case POINTS:
    case EVENTS:
    case OVAL:
      break; 
    case EDIT: 
      if (editMode == MOVING)
      { moveSelected(0,0,x,y); }
      break;
    default:
      break;
    }
    repaint();
  } 
                                                   
  public void mouseMoved(MouseEvent e)
  { // System.out.println("Mouse moved at " + e.getX() + " " + e.getY());
    Object oldselected = selectedComponent; 
 
    findSelected(e.getX(), e.getY());
    if (oldselected != selectedComponent && 
        selectedComponent != null && selectedComponent instanceof ModelElement)
    { ModelElement me = (ModelElement) selectedComponent; 
      parent.setMessage(me.getName() + " has stereotypes: " + me.getStereotypes());  
      if (me instanceof UseCase)
      { UseCase uc = (UseCase) me; 
        parent.setMessage(uc.getName() + " parameters are: " + uc.getParameters()); 
      } 
    } 
  }
  
  
  public void paintComponent(Graphics g) 
  { super.paintComponent(g);  //clear the panel
    g.setFont(new Font("Serif", Font.BOLD, 18));
    if (g instanceof Graphics2D)
    { Graphics2D g2 = (Graphics2D) g;
      drawShapes(g2); 
    } 
    else if (g instanceof PrintGraphics) 
    { drawShapes(g); } 
  }


  public int print(Graphics g, PageFormat pf, int pi)
  throws PrinterException
  { if (pi >= 1) { return Printable.NO_SUCH_PAGE; }
    drawShapes((Graphics2D) g);
    return Printable.PAGE_EXISTS;
  }

  private void drawShapes(Graphics g) 
  { // The number of elements in the drawing area
    int numstates = visuals.size();

    for (int i=0; i < numstates; i++)
    { // get the coordinates
      VisualData vd = (VisualData) visuals.elementAt(i);
      // draw the data elements

      vd.drawData(g); 
    }

    // Draws the line when user drags and 
    // stops but has not
    // released the mouse yet
    if ((mode == SLINES) || (mode == DLINES) || (mode == ACLINES))
    { if (firstpress == true)
      { g.drawLine(x1,y1,x2,y2); }
    }
  }

  private void drawShapes(Graphics2D g2) 
  {  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                        RenderingHints.VALUE_ANTIALIAS_ON );
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, 
                        RenderingHints.VALUE_RENDER_QUALITY );
		
    //Draw a thicker line for the objects
    g2.setStroke(stroke);

    // The number of elements in the drawing area
    int numstates = visuals.size();
    
    // Paint the previous visuals
    for (int i=0; i < numstates; i++) 
    { // get the coordinates	
      VisualData vd = (VisualData) visuals.elementAt(i);
      // draw the data elements
      if (view == DCFD) 
      { vd.drawData(g2); } 
      else 
      { vd.drawCDData(g2); } 
    } 
			
	// Draws the line when user drags and
   // stops but has not
	// released the mouse yet 
    if ((mode == SLINES) || (mode == DLINES) || (mode == ACLINES))
    { if (firstpress == true)
      { if (waypoints.size() == 0)
        { g2.drawLine(x1,y1, x2, y2); }
        else 
        { LinePoint p1 = (LinePoint) waypoints.get(0); 
          g2.drawLine(x1,y1,p1.x,p1.y); 
          for (int i = 1; i < waypoints.size(); i++) 
          { LinePoint p2 = (LinePoint) waypoints.get(i);
            g2.drawLine(p1.x,p1.y,p2.x,p2.y); 
            p1 = p2; 
          }
          g2.drawLine(p1.x,p1.y,x2,y2); 
        }
      }
    }	 
  }

  private void setAppropriateCursor(int mode)
  { if (mode == EDIT)
    { if (editMode == DELETING)
      { setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR)); }
      else if (editMode == MOVING)
      { setCursor(new Cursor(Cursor.MOVE_CURSOR)); }
      else if (editMode == MODIFY)
      { setCursor(new Cursor(Cursor.HAND_CURSOR)); }
    }
    else
    { setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); }
  }


  public Generalisation lookupGeneralisation(Entity esub, Entity esup) 
  { String gname = esub.getName() + "_" + esup.getName(); 
    Generalisation res = (Generalisation) ModelElement.lookupByName(gname, generalisations); 
    return res; 
  } 

  public Association lookupAssociation(Entity e1, Entity e2, String rol2) 
  { for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      Entity ent1 = ast.getEntity1(); 
      Entity ent2 = ast.getEntity2(); 
      String r2 = ast.getRole2(); 
      if (e1.getName().equals(ent1.getName()) && e2.getName().equals(ent2.getName()) && 
          r2.equals(rol2))
      { return ast; } 
    } 
    return null; 
  } 

  public void generateZ3(PrintWriter out)
  { for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      out.println(e.saveAsZ3Data()); 
    } 

    // and assert all local and global invariants

    Vector z3assertions = new Vector(); 
    java.util.Map env = new java.util.HashMap(); 

    for (int p = 0; p < useCases.size(); p++) 
    { if (useCases.get(p) instanceof UseCase) 
      { UseCase uc = (UseCase) useCases.get(p); 
        Vector ucinvs = new Vector(); 
        ucinvs.addAll(uc.getPostconditions());
        ucinvs.addAll(uc.getInvariants()); 

        for (int q = 0; q < ucinvs.size(); q++) 
        { Constraint ucinv = (Constraint) ucinvs.get(q); 
          if (ucinv.getOwner() != null)
          { Entity owner = ucinv.getOwner();
            Type otype = new Type(owner);  
            BasicExpression sourceVar = new BasicExpression(owner.getName().toLowerCase()); 
            sourceVar.setType(otype); 
            sourceVar.setElementType(otype); 
            Expression ante = ucinv.antecedent(); 
            Expression succ = ucinv.succedent(); 
            Expression rante = ante.addReference(sourceVar,otype); 
            Expression rsucc = succ.addReference(sourceVar,otype); 
            Expression z3succ = rsucc.skolemize(sourceVar,env); 

            Expression cexp; 
            if ("true".equals(rante + ""))
            { cexp = z3succ; } 
            else 
            { cexp = new BinaryExpression("=>",rante,z3succ); } 
 
            z3assertions.add(new BinaryExpression("!",
                               new BinaryExpression(":",sourceVar,
                                 new BasicExpression(otype + "")),cexp)); 
          }  
        } 
      } 
    }  

    java.util.Iterator keys = env.keySet().iterator();
    while (keys.hasNext())
    { String k = (String) keys.next(); 
      BinaryExpression ktype = (BinaryExpression) env.get(k);
      out.println("(declare-fun " + k + " (" + ktype.getLeft() + ") " + ktype.getRight() + ")\n"); 
    } 

    for (int v = 0; v < z3assertions.size(); v++) 
    { Expression zexp = (Expression) z3assertions.get(v); 
      out.println("(assert " + zexp.toZ3() + ")"); 
    }

  } 

  public void generateB(PrintWriter out)
  { BComponent bm = generateBSystemTypes(); 
    Vector allsees = new Vector(); 
    out.println(bm + "\n"); 
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.getSuperclass() == null) 
      { BComponent ebm = e.generateB(entities,types); 
        out.println(ebm + "\n\n"); 
        Vector s = ebm.getSees(); 
        allsees = VectorUtil.union(allsees,s); 
      } 
    } 
    buildBController(); 
    bcontroller.unionSees(allsees); 
    bcontroller.removeSees(bcontroller.getIncludes()); 
    out.println(bcontroller); 
  } 

  // add the use cases as operations of Controller

  public void generateMergedB(PrintWriter out)
  { BComponent bm = generateBSystemTypes(); 
    Vector alluses = new Vector();
    Vector allsees = new Vector(); 
    Vector bcomponents = new Vector(); 
 
    out.println(bm + "\n"); 
    BComponent con = new BSystemTypes("Controller");
    
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.getSuperclass() == null) 
      { BComponent ebm = e.generateB(entities,types);
        bcomponents.add(ebm);  
        con.union(ebm,null,e); 
        // out.println(ebm + "\n\n"); 
        Vector s = ebm.getUses(); 
        alluses = VectorUtil.union(alluses,s); 
        allsees.add(e.getName()); 
      } 
    } 
    
    // con.setIncludes(new Vector()); 
    // buildBController(); 
    // bcontroller.unionSees(allsees); 
    con.removeUses(alluses); 
    con.removeSees(allsees); 
    // con.addSees(bm.getName()); 
    bcontroller = con; 
    for (int k = 0; k < invariants.size(); k++)
    { Constraint c = (Constraint) invariants.get(k); 
      if (c.isLocal() || c.getEvent() != null) { } 
      else 
      { Entity cown = c.getOwner(); 
        Vector contexts = new Vector(); 
        contexts.add(cown); 
        c.typeCheck(types,entities,contexts); 
        BExpression inv = c.binvariant(); // for global ones
        bcontroller.addInvariant(inv); 
      } 
    } 

    for (int p = 0; p < useCases.size(); p++) 
    { if (useCases.get(p) instanceof UseCase) 
      { UseCase uc = (UseCase) useCases.get(p); 
        Vector ucinvs = uc.getInvariants();
        for (int q = 0; q < ucinvs.size(); q++) 
        { Constraint ucinv = (Constraint) ucinvs.get(q); 
          BExpression binv = ucinv.binvariant(); 
          bcontroller.addInvariant(binv); 
        } 
      } 
    }  
    out.println(bcontroller); 
  } 


  public BComponent generateBSystemTypes()
  { BSystemTypes sys = new BSystemTypes("SystemTypes");
    for (int i = 0; i < types.size(); i++)
    { Type t = (Type) types.get(i);
      String nme = t.getName();
      Vector values = t.getValues();
      sys.addSetDefinition(nme,values);
    }
    for (int j = 0; j < entities.size(); j++)
    { Entity e = (Entity) entities.get(j);
      if (e.getSuperclass() == null) 
      { String nme = e.getName();
        sys.addSetDefinition(nme + "_OBJ",null);
        sys.addCardinalityBound(nme + "_OBJ",e.getCardinality()); 
      } 
    }
    return sys;
  }

  public void buildBController()
  { BComponent con = new BSystemTypes("Controller");
    // con.addSees("SystemTypes");
    for (int i = 0; i < entities.size(); i++)
    { Entity ent = (Entity) entities.get(i);
      String ename = ent.getName();
      String ex = ename.toLowerCase() + "x";
      String es = ename.toLowerCase() + "s";
      Vector pars = new Vector();
      pars.add(ex);
      if (ent.getSuperclass() == null) 
      { con.addIncludes(ename); } // no inheritance yet
      /* BExpression pre =
        new BBinaryExpression(":",
          new BBasicExpression(ex),
          new BBasicExpression(es)); */
      Vector atts = ent.getAttributes();
      for (int j = 0; j < atts.size(); j++)
      { Attribute att = (Attribute) atts.get(j);
        Vector updates = att.senBOperationsCode(invariants,ent,entities,types); 
        for (int k = 0; k < updates.size(); k++) 
        { BOp op = (BOp) updates.get(k); 
          con.addOperation(op); 
        }  
      }
      Vector asts = ent.getAssociations();
      for (int j = 0; j < asts.size(); j++)
      { Association ast = (Association) asts.get(j);
        Vector updates = ast.senBOperationsCode(invariants,ent,entities,types); 
        for (int k = 0; k < updates.size(); k++) 
        { BOp op = (BOp) updates.get(k); 
          con.addOperation(op); 
        }  
      }
      Vector ops = ent.getOperations(); 
      // for each, add a BOp to controller which calls the
      // entity version:
      for (int w = 0; w < ops.size(); w++)
      { BehaviouralFeature bf = (BehaviouralFeature) ops.get(w);
        BOp bfop = bf.getGlobalBOperationCode(ent,entities,types,constraints); 
        if (bfop != null)
        { con.addOperation(bfop); } 
      }
    }
    bcontroller = con;
    for (int k = 0; k < invariants.size(); k++)
    { Constraint c = (Constraint) invariants.get(k); 
      if (c.isLocal() || c.getEvent() != null) { } 
      else 
      { Entity cown = c.getOwner(); 
        Vector contexts = new Vector(); 
        contexts.add(cown); 
        c.typeCheck(types,entities,contexts); 
        BExpression inv = c.binvariant(); // for global ones
        bcontroller.addInvariant(inv); 
      } 
    } 
  }  // and the use cases. 

  public void generateSmv(PrintWriter out)
  { SmvModule mainMod = SmvModule.createMainModule(entities); 
    SmvModule contMod = SmvModule.createControllerModule(entities); 
    mainMod.display(out);
    out.println(); 
    contMod.display(out); 
    out.println();
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      SmvModule mod = ent.generateSmv(); 
      if (mod != null)
      { mod.display(out); }
    } 
  } 

  public void generateCSharp(PrintWriter out, PrintWriter out2)
  { out.println("using System;"); 
    out.println("using System.Collections;"); 
    out.println("using System.IO;"); 
    out.println("using System.Linq;");
    out.println("using System.Threading.Tasks;");
    out.println("using System.Windows.Forms;\n\n");

    out.println(""); 

    if (systemName != null && systemName.length() > 0)
    { out.println("namespace " + systemName + " {\n\n"); } 
 
    String mainOp = ""; 

    // If any entity has an activity, this becomes the main operation: 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.generateCSharp(entities,types,out); 
      String mop = ent.genMainOperation(entities,types); 
      if (mop != null)
      { mainOp = mop; } 
      out.println(); 
    } 
    out.println(); 

    if (useCases.size() > 0)
    { int nn = useCases.size(); 
      mainOp = ""; 
      for (int j = 0; j < useCases.size(); j++) 
      { ModelElement me = (ModelElement) useCases.get(j); 
        if (me instanceof UseCase) 
        { UseCase uc = (UseCase) me; 
          String nme = uc.getName(); 
          mainOp = mainOp + "\n  " + uc.genOperationCSharp(entities,types) + "\n";
        } 
      } 
    }

    generateControllerCSharp(mainOp,out);


    out.println(); 

    generateSystemTypesCSharp(out); // out2

    String gui = GUIBuilder.generateCSharpGUI(useCases); 
    // File guifile = new File("output/Program.cs");
    try
    { // PrintWriter gout = new PrintWriter(
      //                         new BufferedWriter(
      //                           new FileWriter(guifile)));
      // if (systemName != null && systemName.length() > 0)
      // { gout.println("import " + systemName + ".Controller;\n"); }  
      out.println(gui);  // gout
      // gout.close();
    }
    catch (Exception ex) { }

    if (systemName != null && systemName.length() > 0)
    { out.println("} \n\n"); } 

    try
    { out.close();
      out2.close(); 
    }
    catch (Exception ex) { }

  }

  public void generateCPP(PrintWriter out, PrintWriter out2)
  { out.println("// controller.h"); 
    // out.println("#include <string>"); 
    // out.println("#include <vector>");
    // out.println("#include <set>"); 
    // out.println("#include <map>");
    // out.println("#include <iostream>"); 
    // out.println("#include <cmath>"); 
    // out.println("#include <algorithm>"); 
    
    out.println(""); 
    out.println("using namespace std;\n"); 

    if (systemName != null && systemName.length() > 0)
    { out.println("namespace " + systemName + " {\n\n"); } 
 
    out2.println("// Controller.cc"); 
    out2.println("#include <string>"); 
    out2.println("#include <vector>");
    out2.println("#include <set>"); 
    out2.println("#include <map>");
    out2.println("#include <iostream>"); 
    out2.println("#include <fstream>"); 
    out2.println("#include <cmath>"); 
    out2.println("#include <algorithm>"); 
    out2.println("#include \"controller.h\""); 
 
    out2.println(""); 
    out2.println("using namespace std;\n"); 

    out2.println("Controller* Controller::inst = new Controller();\n\n"); 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.staticAttributeDefinitions(out2);  
    } 

 
    String mainOp = ""; 

    // If any entity has an activity, this becomes the main operation: 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      out.println("class " + ent.getName() + ";"); 
    } 

    Vector orderedByInheritance = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      if (ent.isRoot())
      { ent.levelOrder(orderedByInheritance); }  
    } 
    

    out.println("class Controller;"); 
    out.println(); 
    generateSystemTypesCPP(out); 
    out.println(); 

    for (int i = 0; i < orderedByInheritance.size(); i++) 
    { Entity ent = (Entity) orderedByInheritance.get(i); 
      ent.generateCPP(entities,types,out,out2); 
      String mop = ent.genMainOperation(entities,types); 
      if (mop != null)
      { mainOp = mop; } 
      out.println(); 
    } 
    out.println(); 


    Vector mainOpcodes = new Vector(); 
    if (useCases.size() > 0)
    { int nn = useCases.size(); 
      mainOp = ""; 
      for (int j = 0; j < useCases.size(); j++) 
      { ModelElement me = (ModelElement) useCases.get(j); 
        if (me instanceof UseCase) 
        { UseCase uc = (UseCase) me; 
          String nme = uc.getName(); 
          mainOp = mainOp + "\n  " + uc.genOperationCPP(entities,types,mainOpcodes) + "\n";
        } 
      } 
    }

    // out2.println(BSystemTypes.getForAllOps()); 
    // out2.println(BSystemTypes.getSelectOps());

    generateControllerCPP(mainOp,out,out2);
    
    out2.println(); 

    for (int h = 0; h < mainOpcodes.size(); h++) 
    { String opcode = (String) mainOpcodes.get(h); 
      out2.println(opcode); 
    } 

    if (systemName != null && systemName.length() > 0)
    { out.println("} \n\n"); } 

    try
    { out.close();
      out2.close(); 
    }
    catch (Exception ex) { }

    System.out.println("classes ordered by inheritance are: " + orderedByInheritance);
  }

public void produceCUI(PrintWriter out)
{ out.println("#include \"app.c\"");
  out.println();
  out.println(); 

  out.println("int main(int _argc, char* _argv[])"); 
  out.println("{ char** res = getFileLines(\"app.itf\");");  
  out.println("  int ncommands = length(res);");
  out.println("  int i = 0;");
  out.println("  printf(\"Available use cases are:\\n\");");  
  out.println("  for ( ; i < ncommands; i++)"); 
  out.println("  { printf(res[i]); }"); 
  out.println("  printf(\"Enter the use case to execute as\\n\");"); 
  out.println("  printf(\"name arguments (separated by spaces)\\n\");");
  out.println("  printf(\"\\n\");");
  out.println("  char* cmd = (char*) malloc(1024*sizeof(char));"); 
  out.println("  char* rd = gets(cmd);"); 
  out.println("  while (strcmp(cmd,\"-\") != 0)");
  out.println("  { int j = 0;"); 
  out.println("    for ( ; j < ncommands; j++)"); 
  out.println("    { char** ctok = tokenise(res[j], isspace);"); 
  out.println("      char* uc = ctok[0];"); 
  out.println("      if (startsWith(cmd,uc))");
  out.println("      { char* format = buildFormat(ctok);");
  out.println("        int err = 0;");
  for (int q = 0; q < useCases.size(); q++)
  { ModelElement me = (ModelElement) useCases.get(q);
    if (me instanceof UseCase)
    { UseCase uc = (UseCase) me;
      uc.generateCUIcode(out);
    } 
  }
  out.println("      }");
  out.println("    }");  
  out.println("    printf(\"Next command, or - to end: \\n\");");  
  out.println("    rd = gets(cmd);");   
  out.println("  }");
  out.println("}");
}

  public void generateJava6(PrintWriter out, PrintWriter out2)
  { if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); } 
    out.println("import java.util.*;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.Collection;");
    out.println("import java.util.ArrayList;");
    out.println("import java.util.HashSet;");
    out.println("import java.util.Collections;");
    out.println("import java.util.List;");
    out.println("import java.util.Vector;");
    out.println("import java.lang.*;");
    out.println("import java.lang.reflect.*;"); 
    out.println("import java.util.StringTokenizer;"); 
    out.println("import java.io.*;\n"); 

    String dirName = "output"; 

    if (systemName != null && systemName.length() > 0)
    { dirName = systemName; } 
 
    String mainOp = ""; 

    // If any entity has an activity, this becomes the main operation: 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.generateJava6(entities,types,out); 
      String mop = ent.genMainOperation(entities,types); 
      if (mop != null)
      { mainOp = mop; } 
      out.println(); 
    } 
    out.println(); 

    if (useCases.size() > 0)
    { int nn = useCases.size(); 
      mainOp = ""; 
      for (int j = 0; j < useCases.size(); j++) 
      { ModelElement me = (ModelElement) useCases.get(j); 
        if (me instanceof UseCase) 
        { UseCase uc = (UseCase) me; 
          String nme = uc.getName(); 
          mainOp = mainOp + "\n  " + uc.genOperationJava6(entities,types) + "\n";
        } 
      } 
    }

    generateControllerJava6(mainOp,out);

    generateSystemTypesJava6(out2); 

    String gui = GUIBuilder.buildUCGUIJava6(useCases,"",false); 
    File guifile = new File(dirName + "/GUI.java");
    try
    { PrintWriter gout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(guifile)));
      if (systemName != null && systemName.length() > 0)
      { gout.println("package " + systemName + ";\n\n"); }  
      gout.println(gui); 
      gout.close();
    }
    catch (Exception ex) { }

    try
    { out.close();
      out2.close(); 
    }
    catch (Exception ex) { }

  }

  public void generateJava7(PrintWriter out, PrintWriter out2)
  { String dirName = "output"; 

    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); 
      dirName = systemName; 
    } 

    out.println("import java.util.*;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.Collection;");
    out.println("import java.util.List;");
    out.println("import java.util.ArrayList;");
    out.println("import java.util.Set;");
    out.println("import java.util.HashSet;");
    out.println("import java.util.TreeSet;");
    out.println("import java.util.Collections;");
    out.println("import java.lang.*;");
    out.println("import java.lang.reflect.*;"); 
    out.println("import java.util.StringTokenizer;"); 
    out.println("import java.io.*;\n"); 

 
    String mainOp = ""; 

    // If any entity has an activity, this becomes the main operation: 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.generateJava7(entities,types,out); 
      String mop = ent.genMainOperation(entities,types); 
      if (mop != null)
      { mainOp = mop; } 
      out.println(); 
    } 
    out.println(); 

    if (useCases.size() > 0)
    { int nn = useCases.size(); 
      mainOp = ""; 
      for (int j = 0; j < useCases.size(); j++) 
      { ModelElement me = (ModelElement) useCases.get(j); 
        if (me instanceof UseCase) 
        { UseCase uc = (UseCase) me; 
          String nme = uc.getName(); 
          mainOp = mainOp + "\n  " + uc.genOperationJava7(entities,types) + "\n";
        } 
      } 
    }

    generateControllerJava7(mainOp,out);

    generateSystemTypesJava7(out2); 

    String gui = GUIBuilder.buildUCGUIJava6(useCases,"",false); 
    File guifile = new File(dirName + "/GUI.java");
    try
    { PrintWriter gout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(guifile)));
      if (systemName != null && systemName.length() > 0)
      { gout.println("package " + systemName + ";\n\n"); }  
      gout.println(gui); 
      gout.close();
    }
    catch (Exception ex) { }

    try
    { out.close();
      out2.close(); 
    }
    catch (Exception ex) { }

  }

  public void printJava4Header(PrintWriter out)
  { out.println("import java.util.List;"); 
    out.println("import java.util.Date;"); 
    out.println("import java.util.Map;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.Vector;\n");
    out.println("import java.lang.*;");
    out.println("import java.lang.reflect.*;"); 
    out.println("import java.util.StringTokenizer;"); 
    out.println("import java.io.*;\n"); 
 
    for (int i = 0; i < importList.size(); i++) 
    { String imprt = (String) importList.get(i); 
      out.println(imprt); 
    } 
    out.println("\n"); 
  } 

  public void printJava6Header(PrintWriter out)
  { out.println("import java.util.Date;"); 
    out.println("import java.util.Vector;"); 
    out.println("import java.util.ArrayList;"); 
    out.println("import java.util.Map;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.HashSet;"); 
    out.println("import java.util.TreeSet;"); 
    out.println("import java.util.Collection;\n");
    out.println("import java.util.Collections;");
    out.println("import java.util.List;");
    out.println("import java.lang.*;");
    out.println("import java.lang.reflect.*;"); 
    out.println("import java.util.StringTokenizer;"); 
    out.println("import java.io.*;\n"); 
 
    for (int i = 0; i < importList.size(); i++) 
    { String imprt = (String) importList.get(i); 
      out.println(imprt); 
    } 
    out.println("\n"); 
  } 

  public void printJava7Header(PrintWriter out)
  { out.println("import java.util.Date;"); 
    out.println("import java.util.Vector;"); 
    out.println("import java.util.List;"); 
    out.println("import java.util.ArrayList;"); 
    out.println("import java.util.Map;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.Set;"); 
    out.println("import java.util.TreeSet;"); 
    out.println("import java.util.HashSet;"); 
    out.println("import java.util.Collection;\n");
    out.println("import java.util.Collections;\n");
    out.println("import java.lang.*;");
    out.println("import java.lang.reflect.*;"); 
    out.println("import java.util.StringTokenizer;"); 
    out.println("import java.io.*;\n"); 
 
    for (int i = 0; i < importList.size(); i++) 
    { String imprt = (String) importList.get(i); 
      out.println(imprt); 
    } 
    out.println("\n"); 
  } 

  public void printCSharpHeader(PrintWriter out)
  { out.println("using System;"); 
    out.println("using System.Collections;"); 
    out.println("using System.IO;"); 
    out.println("using System.Linq;");  
    out.println("using System.Threading.Tasks;"); 
    out.println("using System.Windows.Forms;");

    for (int i = 0; i < importList.size(); i++) 
    { String imprt = (String) importList.get(i); 
      out.println(imprt); 
    } 
    out.println("\n"); 
  } 
        
  public void exportClasses(PrintWriter out, PrintWriter out2, PrintWriter out3)
  { String dirName = "output"; 

    if (systemName != null && systemName.length() > 0) 
    { dirName = systemName; } 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      String nme = ent.getName();   
      if (ent.isExternal() || ent.isExternalApp()) 
      { }
      else 
      { try
        { PrintWriter cout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(dirName + "/" + nme + ".java")));
          exportClass(ent,cout); 
          cout.close();
        }
        catch (Exception ex) { }
      } 
    } 

    boolean incr = isIncremental(); 
    String mainOp = ""; 

    if (useCases.size() > 0)
    { int nn = useCases.size(); 
      
      for (int j = 0; j < nn; j++) 
      { ModelElement me = (ModelElement) useCases.get(j); 
        if (me instanceof UseCase) 
        { UseCase uc = (UseCase) me; 
          String nme = uc.getName(); 
          mainOp = mainOp + "\n  " + uc.genOperation(entities,types) + "\n";
        } 
      } 
    } 

    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); }

    printJava4Header(out); 
    generateController(mainOp,out,out3,incr);
    out.println();  
    generateSystemTypes(out2);

    String gui = GUIBuilder.buildUCGUI(useCases,"",incr); 
    File guifile = new File(dirName + "/GUI.java");
    try
    { PrintWriter gout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(guifile)));
      if (systemName != null && systemName.length() > 0)
      { gout.println("package " + systemName + ";\n\n"); }  
      gout.println(gui); 
      gout.close();
    }
    catch (Exception ex) { }

  } 

  public void exportClass(Entity ent, PrintWriter out)
  { if (ent.isExternal() || ent.isExternalApp()) 
    { return; }
 
    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); }

    printJava4Header(out); 
    out.print("public "); 
    ent.generateJava(entities,types,out); 
  }  

  public void exportClassesJava6(PrintWriter out, PrintWriter out2, PrintWriter out3)
  { 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      String nme = ent.getName();   
      if (ent.isExternal() || ent.isExternalApp()) 
      { }
      else 
      { try
        { PrintWriter cout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter("output/" + nme + ".java")));
          exportClassJava6(ent,cout); 
          cout.close();
        }
        catch (Exception ex) { }
      } 
    } 
    // boolean incr = isIncremental(); 

    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); }

    printJava6Header(out); 
    generateControllerJava6("",out);
    out.println();  
    generateSystemTypesJava6(out2);

    String gui = GUIBuilder.buildUCGUIJava6(useCases,systemName,false); 
    File guifile = new File("output/GUI.java");
    try
    { PrintWriter gout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(guifile)));
      // if (systemName != null && systemName.length() > 0)
      // { gout.println("import " + systemName + ".Controller;\n"); }  
      gout.println(gui); 
      gout.close();
    }
    catch (Exception ex) { }
  } 

  public void exportClassJava6(Entity ent, PrintWriter out)
  { if (ent.isExternal() || ent.isExternalApp()) 
    { return; }
 
    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); }

    printJava6Header(out); 
    out.print("public "); 
    ent.generateJava6(entities,types,out); 
  }  

  public void exportClassesJava7(PrintWriter out, PrintWriter out2, PrintWriter out3)
  { 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      String nme = ent.getName();   
      if (ent.isExternal() || ent.isExternalApp()) 
      { }
      else 
      { try
        { PrintWriter cout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter("output/" + nme + ".java")));
          exportClassJava7(ent,cout); 
          cout.close();
        }
        catch (Exception ex) { }
      } 
    } 
    // boolean incr = isIncremental(); 

    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); }

    printJava7Header(out); 
    generateControllerJava7("",out);
    out.println();  
    generateSystemTypesJava7(out2);

    String gui = GUIBuilder.buildUCGUIJava6(useCases,systemName,false); 
    File guifile = new File("output/GUI.java");
    try
    { PrintWriter gout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(guifile)));
      // if (systemName != null && systemName.length() > 0)
      // { gout.println("import " + systemName + ".Controller;\n"); }  
      gout.println(gui); 
      gout.close();
    }
    catch (Exception ex) { }
  } 

  public void exportClassJava7(Entity ent, PrintWriter out)
  { if (ent.isExternal() || ent.isExternalApp()) 
    { return; }
 
    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); }

    printJava7Header(out); 
    out.print("public "); 
    ent.generateJava7(entities,types,out); 
  }  

  public void exportClassesCSharp(PrintWriter out, PrintWriter out2, PrintWriter out3)
  { 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      String nme = ent.getName();   
      if (ent.isExternal() || ent.isExternalApp()) 
      { }
      else 
      { try
        { PrintWriter cout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter("output/" + nme + ".cs")));
          exportClassCSharp(ent,cout); 
          cout.close();
        }
        catch (Exception ex) { }
      } 
    } 
    // boolean incr = isIncremental(); 

    if (systemName != null && systemName.length() > 0)
    { out.println("namespace " + systemName + " { \n\n"); }

    printCSharpHeader(out); 
    out.println(); 
    out.print("public "); 
    generateControllerCSharp("",out);
    out.println();  
    if (systemName != null && systemName.length() > 0)
    { out.println(" } \n\n"); }

    // out2.println(); 
    // out2.print("public "); 
    out2.println("using System;"); 
    out2.println("using System.Collections;\n\n");
    generateSystemTypesCSharp(out2);
  } 

  public void exportClassCSharp(Entity ent, PrintWriter out)
  { if (ent.isExternal() || ent.isExternalApp()) 
    { return; }
 
    if (systemName != null && systemName.length() > 0)
    { out.println("namespace " + systemName + " { \n\n"); }

    printCSharpHeader(out); 
    out.print("public "); 
    ent.generateCSharp(entities,types,out); 

    if (systemName != null && systemName.length() > 0)
    { out.println(" } \n\n"); }
  }  

  public void generateJava(PrintWriter out, PrintWriter out2, PrintWriter out3)
  { String dirName = "output"; 

    if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n\n"); 
      dirName = systemName; 
    } 

    printJava4Header(out); 

    String mainOp = ""; 

    // If any entity has an activity, this becomes the main operation: 

    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.generateJava(entities,types,out); 
      String mop = ent.genMainOperation(entities,types); 
      if (mop != null)
      { mainOp = mop; } 
      out.println(); 
    } 
    out.println(); 

    // But the last use case will define the main op if there is a use case:

    if (useCases.size() > 0)
    { int nn = useCases.size(); 
      mainOp = ""; 
      for (int j = 0; j < nn; j++) 
      { ModelElement me = (ModelElement) useCases.get(j); 
        if (me instanceof UseCase) 
        { UseCase uc = (UseCase) me; 
          String nme = uc.getName(); 
          mainOp = mainOp + "\n  " + uc.genOperation(entities,types) + "\n";
        } 
      } 
      /* UseCase lastuc = (UseCase) useCases.get(nn - 1); 
      mainOp = mainOp + 
          "  public static void main(String[] args)\n" + 
          "  { \n" + 
          "    Controller.inst()." + lastuc.getName() + "();\n" + 
          "  }\n\n";  */ 
    } 

    boolean incr = isIncremental(); 

    generateController(mainOp,out,out3,incr);
    out.println();  
    generateSystemTypes(out2); 
    String gui = GUIBuilder.buildUCGUI(useCases,"",incr); 
    File guifile = new File(dirName + "/GUI.java");
    try
    { PrintWriter gout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(guifile)));

      if (systemName != null && systemName.length() > 0)
      { gout.println("package " + systemName + ";\n\n"); }  

      gout.println(gui); 
      gout.close();
    }
    catch (Exception ex) { }
  } 

  private boolean isIncremental()  // if there is some incremental use case
  { int nn = useCases.size(); 
    if (nn == 0) { return false; } 

    for (int j = 0; j < nn; j++) 
    { ModelElement me = (ModelElement) useCases.get(j); 
      if (me instanceof UseCase) 
      { UseCase uc = (UseCase) me; 
        if (uc.isIncremental()) { return true; } 
      }
    } 
    return false; 
  } 

  private void generateSystemTypes(PrintWriter out)
  { if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n"); } 

    out.println("import java.util.Date;"); 
    out.println("import java.util.List;"); 
    out.println("import java.util.Map;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.Vector;\n");
    out.println("public interface SystemTypes");
    out.println("{");
    for (int i = 0; i < types.size(); i++)
    { Type t = (Type) types.get(i);
      t.generateDeclaration(out);
    }
   
    // should use real sets
    out.println("  public class Set"); 
    out.println("  { private Vector elements = new Vector();\n"); 
    String sops = BSystemTypes.getSelectOps();
    out.println(sops);

    String rops = BSystemTypes.getRejectOps();
    out.println(rops);

    String exops = BSystemTypes.getExistsOps();
    out.println(exops);

    String ex1ops = BSystemTypes.getExists1Ops();
    out.println(ex1ops);

    String faops = BSystemTypes.getForAllOps();
    out.println(faops);

    String collops = BSystemTypes.getCollectOps();
    out.println(collops);

    out.println(BSystemTypes.generateSetEqualsOp()); 
    out.println("    public Set add(Object x)"); 
    out.println("    { if (x != null) { elements.add(x); }"); 
    out.println("      return this; }\n"); 
    out.println("    public Set add(int x)"); 
    out.println("    { elements.add(new Integer(x));"); 
    out.println("      return this; }\n"); 
    out.println("    public Set add(long x)"); 
    out.println("    { elements.add(new Long(x));"); 
    out.println("      return this; }\n"); 
    out.println("    public Set add(double x)"); 
    out.println("    { elements.add(new Double(x));"); 
    out.println("      return this; }\n"); 
    out.println("    public Set add(boolean x)"); 
    out.println("    { elements.add(new Boolean(x));"); 
    out.println("      return this; }\n"); 
    out.println("    public List getElements() { return elements; }\n"); 
    
    String mop = BSystemTypes.generateMaxOp(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateMinOp();
    out.println("\n" + mop);
    mop = BSystemTypes.generateUnionOp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateSubtractOp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateIntersectionOp();
    out.println("\n" + mop);
    mop = BSystemTypes.symmetricDifferenceOp();
    out.println("\n" + mop);
    mop = BSystemTypes.generateIsUniqueOp();
    out.println("\n" + mop);

    mop = BSystemTypes.generateSumOps();
    out.println("\n" + mop);
    mop = BSystemTypes.generatePrdOps();
    out.println("\n" + mop);
    mop = BSystemTypes.generateConcatOp(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateClosureOps(associations); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateAsSetOp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateReverseOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFrontOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateTailOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFirstOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateLastOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortByOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubrangeOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.prependOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.appendOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.countOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.charactersOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateAnyOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubcollectionsOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.maximalElementsOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.minimalElementsOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectAllOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionAllOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateConcatAllOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateInsertAtOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsIntegerOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsRealOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsLongOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsTypeOfOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateTokeniseCSVOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateBeforeOp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateAfterOp(); 
    out.println("\n" + mop);


    /* Map operations - optional */ 

    mop = BSystemTypes.generateIncludesAllMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludesAllMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIncludingMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludeAllMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapKeyOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapValueOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectionMapOp(); 
    out.println("\n" + mop);  

    out.println("  }"); 

    out.println("}");
  }

  private void generateSystemTypesJava6(PrintWriter out)
  { if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n"); } 

    out.println("import java.util.Date;"); 
    out.println("import java.util.ArrayList;"); 
    out.println("import java.util.Map;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.HashSet;"); 
    out.println("import java.util.Collection;\n");
    out.println("import java.util.Collections;");
    out.println("import java.util.List;");
    out.println("import java.util.Vector;\n");

    out.println("public interface SystemTypes");
    out.println("{");
    for (int i = 0; i < types.size(); i++)
    { Type t = (Type) types.get(i);
      t.generateDeclaration(out);
    }
   
    // should use real sets
    out.println("  public class Set"); 
    out.println("  { \n"); 
    String sops = BSystemTypes.getSelectOps();
    out.println(sops);

    out.println(BSystemTypes.getRejectOps());

    String exops = BSystemTypes.getExistsOps();
    out.println(exops);

    String ex1ops = BSystemTypes.getExists1Ops();
    out.println(ex1ops);

    String faops = BSystemTypes.getForAllOps();
    out.println(faops);

    String collops = BSystemTypes.getCollectOps();
    out.println(collops);

    // out.println(BSystemTypes.generateSetEqualsOp()); 
    out.println("    public static HashSet addSet(HashSet s, Object x)"); 
    out.println("    { if (x != null) { s.add(x); }"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet addSet(HashSet s, int x)"); 
    out.println("    { s.add(new Integer(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet addSet(HashSet s, long x)"); 
    out.println("    { s.add(new Long(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet addSet(HashSet s, double x)"); 
    out.println("    { s.add(new Double(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet addSet(HashSet s, boolean x)"); 
    out.println("    { s.add(new Boolean(x));"); 
    out.println("      return s; }\n"); 

    out.println("    public static ArrayList addSequence(ArrayList s, Object x)"); 
    out.println("    { if (x != null) { s.add(x); }"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList addSequence(ArrayList s, int x)"); 
    out.println("    { s.add(new Integer(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList addSequence(ArrayList s, long x)"); 
    out.println("    { s.add(new Long(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList addSequence(ArrayList s, double x)"); 
    out.println("    { s.add(new Double(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList addSequence(ArrayList s, boolean x)"); 
    out.println("    { s.add(new Boolean(x));"); 
    out.println("      return s; }\n"); 
    // Surely these should have ArrayList, not HashSet???

    out.println("    public static ArrayList asSequence(Collection c)"); 
    out.println("    { ArrayList res = new ArrayList(); res.addAll(c); return res; }\n"); 
    out.println("    public static HashSet asSet(Collection c)"); 
    out.println("    { HashSet res = new HashSet(); res.addAll(c); return res; }\n"); 
    
    String mop = BSystemTypes.generateMaxOpJava6(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateMinOpJava6();
    out.println("\n" + mop);
    mop = BSystemTypes.generateUnionOpJava6(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateSubtractOpJava6(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateIntersectionOpJava6();
    out.println("\n" + mop);
    mop = BSystemTypes.symmetricDifferenceOpJava6();
    out.println("\n" + mop);
    mop = BSystemTypes.generateIsUniqueOpJava6();
    out.println("\n" + mop);

    mop = BSystemTypes.generateSumOpsJava6();
    out.println("\n" + mop);
    mop = BSystemTypes.generatePrdOpsJava6();
    out.println("\n" + mop);
    mop = BSystemTypes.generateConcatOpJava6(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateClosureOpsJava6(associations); 
    out.println("\n" + mop); 
    // mop = BSystemTypes.generateAsSetOp(); 
    // out.println("\n" + mop);
    mop = BSystemTypes.generateReverseOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFrontOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateTailOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortByOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubrangeOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.prependOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.appendOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.countOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.charactersOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateAnyOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFirstOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateLastOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubcollectionsOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.maximalElementsOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.minimalElementsOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectAllOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionAllOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateConcatAllOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateInsertAtOpJava6(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsIntegerOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsRealOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsLongOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsTypeOfOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateBeforeOp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateAfterOp(); 
    out.println("\n" + mop);

    /* Map operations - optional */ 

    mop = BSystemTypes.generateIncludesAllMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludesAllMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIncludingMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludeAllMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapKeyOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapValueOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionMapOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectionMapOp(); 
    out.println("\n" + mop);  

    out.println("  }"); 

    out.println("}");
  }


  private void generateSystemTypesJava7(PrintWriter out)
  { if (systemName != null && systemName.length() > 0)
    { out.println("package " + systemName + ";\n"); } 

    out.println("import java.util.Date;"); 
    out.println("import java.util.List;"); 
    out.println("import java.util.ArrayList;"); 
    out.println("import java.util.Map;"); 
    out.println("import java.util.HashMap;"); 
    out.println("import java.util.Set;"); 
    out.println("import java.util.TreeSet;"); 
    out.println("import java.util.HashSet;"); 
    out.println("import java.util.Collection;");
    out.println("import java.util.Collections;\n\n");
    
    out.println("public interface SystemTypes");
    out.println("{");
    for (int i = 0; i < types.size(); i++)
    { Type t = (Type) types.get(i);
      t.generateDeclaration(out);
    }
   
    // should use real sets
    out.println("  public class Ocl"); 
    out.println("  { \n"); 
    String sops = BSystemTypes.getSelectOps();
    out.println(sops);

    out.println(BSystemTypes.getRejectOps());

    String exops = BSystemTypes.getExistsOps();
    out.println(exops);

    String ex1ops = BSystemTypes.getExists1Ops();
    out.println(ex1ops);

    String faops = BSystemTypes.getForAllOps();
    out.println(faops);

    String collops = BSystemTypes.getCollectOps();
    out.println(collops);

    // out.println(BSystemTypes.generateSetEqualsOp()); 
    out.println("    public static <T> HashSet<T> addSet(HashSet<T> s, T x)"); 
    out.println("    { if (x != null) { s.add(x); }"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet<Integer> addSet(HashSet<Integer> s, int x)"); 
    out.println("    { s.add(new Integer(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet<Double> addSet(HashSet<Double> s, double x)"); 
    out.println("    { s.add(new Double(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet<Long> addSet(HashSet<Long> s, long x)"); 
    out.println("    { s.add(new Long(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static HashSet<Boolean> addSet(HashSet<Boolean> s, boolean x)"); 
    out.println("    { s.add(new Boolean(x));"); 
    out.println("      return s; }\n"); 

    out.println("    public static <T> ArrayList<T> addSequence(ArrayList<T> s, T x)"); 
    out.println("    { if (x != null) { s.add(x); }"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList<Integer> addSequence(ArrayList<Integer> s, int x)"); 
    out.println("    { s.add(new Integer(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList<Double> addSequence(ArrayList<Double> s, double x)"); 
    out.println("    { s.add(new Double(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList<Long> addSequence(ArrayList<Long> s, long x)"); 
    out.println("    { s.add(new Long(x));"); 
    out.println("      return s; }\n"); 
    out.println("    public static ArrayList<Boolean> addSequence(ArrayList<Boolean> s, boolean x)"); 
    out.println("    { s.add(new Boolean(x));"); 
    out.println("      return s; }\n"); 
    // Surely these should have ArrayList, not HashSet???

    out.println("    public static <T> ArrayList<T> asSequence(Collection<T> c)"); 
    out.println("    { ArrayList<T> res = new ArrayList<T>(); res.addAll(c); return res; }\n"); 
    out.println("    public static <T> HashSet<T> asSet(Collection<T> c)"); 
    out.println("    { HashSet res = new HashSet<T>(); res.addAll(c); return res; }\n"); 
    
    // String mop = BSystemTypes.generateMaxOpJava7(); 
    // out.println("\n" + mop); 
    // mop = BSystemTypes.generateMinOpJava7();
    // out.println("\n" + mop);
    String mop = BSystemTypes.generateUnionOpJava7(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateSubtractOpJava7(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateIntersectionOpJava7();
    out.println("\n" + mop);
    mop = BSystemTypes.symmetricDifferenceOpJava7();
    out.println("\n" + mop);
    mop = BSystemTypes.generateIsUniqueOpJava7();
    out.println("\n" + mop);

    mop = BSystemTypes.generateSumOpsJava7();
    out.println("\n" + mop);
    mop = BSystemTypes.generatePrdOpsJava7();
    out.println("\n" + mop);
    mop = BSystemTypes.generateConcatOpJava7(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateClosureOpsJava7(associations); 
    out.println("\n" + mop); 
    // mop = BSystemTypes.generateAsSetOp(); 
    // out.println("\n" + mop);
    mop = BSystemTypes.generateReverseOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFrontOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateTailOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortByOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubrangeOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.prependOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.appendOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.countOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.charactersOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateAnyOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFirstOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateLastOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubcollectionsOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.maximalElementsOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.minimalElementsOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectAllOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionAllOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateConcatAllOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateInsertAtOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsIntegerOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsRealOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsLongOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsTypeOfOp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateBeforeOp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateAfterOp(); 
    out.println("\n" + mop);

    /* Map operations - optional */ 

    // mop = BSystemTypes.generateIncludesAllMapOpJava7(); 
    // out.println("\n" + mop);  
    // mop = BSystemTypes.generateExcludesAllMapOpJava7(); 
    // out.println("\n" + mop);  
    mop = BSystemTypes.generateIncludingMapOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludeAllMapOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapKeyOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapValueOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionMapOpJava7(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectionMapOpJava7(); 
    out.println("\n" + mop);  

    out.println("  }"); 

    out.println("}");
  }

  private void generateSystemTypesCSharp(PrintWriter out)
  { // out.println("using System;"); 
    // out.println("using System.Collections;\n\n");

    if (systemName != null && systemName.length() > 0)
    { out.println("namespace " + systemName + " {\n\n"); } 

    for (int i = 0; i < types.size(); i++)
    { Type t = (Type) types.get(i);
      t.generateDeclarationCSharp(out);
    }

    out.println("\n\n");
 
    out.println("public class SystemTypes");
    out.println("{");
   
    // should use real sets
    // out.println("  public class Set"); 
    // out.println("  { private ArrayList elements = new ArrayList();\n"); 
    String sops = BSystemTypes.getSelectOps();
    out.println(sops);

    out.println(BSystemTypes.getRejectOps());

    String exops = BSystemTypes.getExistsOps();
    out.println(exops);

    String ex1ops = BSystemTypes.getExists1Ops();
    out.println(ex1ops);

    String faops = BSystemTypes.getForAllOps();
    out.println(faops);

    String collops = BSystemTypes.getCollectOps();
    out.println(collops);

    out.println(BSystemTypes.generateSetEqualsOpCSharp()); 
    out.println("    public static ArrayList addSet(ArrayList a, object x)"); 
    out.println("    { ArrayList res = new ArrayList();"); 
    out.println("      res.AddRange(a); if (x != null) { res.Add(x); }"); 
    out.println("      return res; }\n"); 
    out.println("    public static ArrayList makeSet(object x)"); 
    out.println("    { ArrayList res = new ArrayList();"); 
    out.println("      if (x != null) { res.Add(x); }"); 
    out.println("      return res; }\n"); 
    out.println("    public static ArrayList removeSet(ArrayList a, object x)"); 
    out.println("    { ArrayList res = new ArrayList(); "); 
    out.println("      res.AddRange(a);"); 
    out.println("      while (res.Contains(x)) { res.Remove(x); }"); 
    out.println("      return res; }\n"); 
    
    String mop = BSystemTypes.generateMaxOpCSharp(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateMinOpCSharp();
    out.println("\n" + mop);
    mop = BSystemTypes.generateUnionOpCSharp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateSubtractOpCSharp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateIntersectionOpCSharp();
    out.println("\n" + mop);
    mop = BSystemTypes.symmetricDifferenceOpCSharp();
    out.println("\n" + mop);
    mop = BSystemTypes.generateIsUniqueOpCSharp();
    out.println("\n" + mop);

    mop = BSystemTypes.generateSumOpsCSharp();
    out.println("\n" + mop);
    mop = BSystemTypes.generatePrdOpsCSharp();
    out.println("\n" + mop);
    mop = BSystemTypes.generateConcatOpCSharp(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateClosureOpsCSharp(associations); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateAsSetOpCSharp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateReverseOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFrontOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateTailOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortByOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubrangeOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.countOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.charactersOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateAnyOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFirstOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateLastOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSubcollectionsOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.maximalElementsOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.minimalElementsOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectAllOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionAllOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateInsertAtOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsIntegerOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsRealOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsLongOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateBeforeOpCSharp(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateAfterOpCSharp(); 
    out.println("\n" + mop);

    /* Map operations - optional */ 

    mop = BSystemTypes.generateIncludesAllMapOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludesAllMapOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIncludingMapOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludeAllMapOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapKeyOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapValueOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionMapOpCSharp(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectionMapOpCSharp(); 
    out.println("\n" + mop);  

    // out.println("  }"); 

    out.println("}");
    if (systemName != null && systemName.length() > 0)
    { out.println("}\n\n"); } 

  }

  private void generateSystemTypesCPP(PrintWriter out)
  { // if (systemName != null && systemName.length() > 0)
    // { out.println("namespace " + systemName + ";\n"); } 

    // this library is defined after forward declarations of all classes + 
    // Controller, in Controller.h

    for (int i = 0; i < types.size(); i++)
    { Type t = (Type) types.get(i);
      t.generateDeclarationCPP(out);
    }

    out.println(); 
   
    out.println("template<class _T>"); 
    out.println("class UmlRsdsLib { ");
    out.println("public: "); 
    
    // prototypes first
    
    // should use real sets
    String sops = BSystemTypes.getSelectOps();
    out.println(sops);
    
    out.println(BSystemTypes.getRejectOps());

    String exops = BSystemTypes.getExistsOps();
    out.println(exops);

    String ex1ops = BSystemTypes.getExists1Ops();
    out.println(ex1ops);

    // String faops = BSystemTypes.getForAllDecs();
    // out.println(faops);

    String collops = BSystemTypes.getCollectOps();
    out.println(collops);

    out.println("    static bool isIn(_T x, set<_T>* st)"); 
    out.println("    { return (st->find(x) != st->end()); }\n"); 
    out.println("    static bool isIn(_T x, vector<_T>* sq)"); 
    out.println("    { return (find(sq->begin(), sq->end(), x) != sq->end()); }\n"); 

    out.println("    static bool isSubset(set<_T>* s1, set<_T>* s2)"); 
    out.println("    { bool res = true; "); 
    out.println("      for (set<_T>::iterator _pos = s1->begin(); _pos != s1->end(); ++_pos)"); 
    out.println("      { if (isIn(*_pos, s2)) { } else { return false; } }"); 
    out.println("      return res; }\n\n"); 
    out.println("    static bool isSubset(set<_T>* s1, vector<_T>* s2)"); 
    out.println("    { bool res = true; "); 
    out.println("      for (set<_T>::iterator _pos = s1->begin(); _pos != s1->end(); ++_pos)"); 
    out.println("      { if (isIn(*_pos, s2)) { } else { return false; } }"); 
    out.println("      return res; }\n\n"); 

    out.println("    static set<_T>* makeSet(_T x)"); 
    out.println("    { set<_T>* res = new set<_T>();"); 
    out.println("      if (x != NULL) { res->insert(x); }"); 
    out.println("      return res;"); 
    out.println("    }\n"); 
    out.println("    static vector<_T>* makeSequence(_T x)"); 
    out.println("    { vector<_T>* res = new vector<_T>();"); 
    out.println("      if (x != NULL) { res->push_back(x); } return res;"); 
    out.println("    }\n"); 
    out.println("    static set<_T>* addSet(set<_T>* s, _T x)"); 
    out.println("    { if (x != NULL) { s->insert(x); }"); 
    out.println("      return s; }\n"); 
    out.println("    static vector<_T>* addSequence(vector<_T>* s, _T x)"); 
    out.println("    { if (x != NULL) { s->push_back(x); }"); 
    out.println("      return s; }\n"); 

    out.println("    static vector<_T>* asSequence(set<_T>* c)"); 
    out.println("    { vector<_T>* res = new vector<_T>();");
    out.println("      for (set<_T>::iterator _pos = c->begin(); _pos != c->end(); ++_pos)"); 
    out.println("      { res->push_back(*_pos); } "); 
    out.println("      return res; }\n"); 
    out.println("    static set<_T>* asSet(vector<_T>* c)"); 
    out.println("    { set<_T>* res = new set<_T>(); "); 
    out.println("      for (vector<_T>::iterator _pos = c->begin(); _pos != c->end(); ++_pos)"); 
    out.println("      { res->insert(*_pos); } "); 
    out.println("      return res; \n"); 
    out.println("    }\n"); 

    String mop = BSystemTypes.generateTokeniseOpCPP(); 
    out.println("\n" + mop); 
    
    mop = BSystemTypes.generateRoundOpCPP(); 
    out.println("\n" + mop); 

    mop = BSystemTypes.generateMaxOpCPP(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateMinOpCPP();
    out.println("\n" + mop);
    mop = BSystemTypes.generateUnionOpCPP(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateSubtractOpCPP(); 
    out.println("\n" + mop);
    mop = BSystemTypes.generateIntersectionOpCPP();
    out.println("\n" + mop);
    mop = BSystemTypes.symmetricDifferenceOpCPP();
    out.println("\n" + mop);
    mop = BSystemTypes.generateIsUniqueOpCPP();
    out.println("\n" + mop);

    mop = BSystemTypes.generateSumOpsCPP();
    out.println("\n" + mop);
    mop = BSystemTypes.generatePrdOpsCPP();
    out.println("\n" + mop);
    mop = BSystemTypes.generateConcatOpCPP(); 
    out.println("\n" + mop); 
    mop = BSystemTypes.generateClosureOpsCPP(associations); 
    out.println("\n" + mop); 
    // mop = BSystemTypes.generateAsSetOp(); 
    // out.println("\n" + mop);
    mop = BSystemTypes.generateReverseOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFrontOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateTailOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSortOpCPP(); 
    out.println("\n" + mop);  
    // mop = BSystemTypes.generateSortByOpJava6(); 
    // out.println("\n" + mop);  
    mop = BSystemTypes.generateSubrangeOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.prependOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.appendOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.countOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.charactersOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateAnyOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateFirstOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateLastOpCPP(); 
    out.println("\n" + mop);  
    // mop = BSystemTypes.generateSubcollectionsOp(); 
    // out.println("\n" + mop);  
    mop = BSystemTypes.maximalElementsOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.minimalElementsOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectAllOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionAllOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateInsertAtOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIndexOfOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUCLCOpsCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateSWEWOpsCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsIntegerOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsRealOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIsLongOpCPP(); 
    out.println("\n" + mop);  

    /* Map operations - optional */ 

    mop = BSystemTypes.generateIncludesAllMapOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludesAllMapOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIncludingMapOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludeAllMapOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapKeyOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateExcludingMapValueOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateUnionMapOpCPP(); 
    out.println("\n" + mop);  
    mop = BSystemTypes.generateIntersectionMapOpCPP(); 
    out.println("\n" + mop);  

    out.println("};");
  }


  private void generateController(String mop, PrintWriter out, PrintWriter out3, boolean incr)
  { String addops = "\n";
    String deleteops = "\n"; 
    String createops = "\n"; 
    String globalops = "\n";   

    String controllerInterface = "public interface ControllerInterface\n" + "{\n"; 

    if (systemName != null && systemName.length() > 0)
    { out3.println("package " + systemName + ";\n"); } 

    out3.println("import java.util.Date;"); 
    out3.println("import java.util.List;"); 
    out3.println("import java.util.Map;"); 
    out3.println("import java.util.HashMap;"); 
    out3.println("import java.util.Vector;\n");

    /* Add all implicit associations: */ 

    Vector assocs = new Vector(); 
    assocs.addAll(associations); 
    for (int h = 0; h < entities.size(); h++) 
    { Entity ee = (Entity) entities.get(h); 
      assocs.addAll(ee.allDefinedEntityInstanceAttributes()); 
    } 
        
    out.println("public class Controller " +
                "implements SystemTypes, ControllerInterface");
    out.println("{");
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Entity es = e.getSuperclass(); 
      String nme = e.getName();
      String lcnme = nme.toLowerCase();
      lcnme = lcnme + "s";
      out.println("  Vector " + lcnme +
                         " = new Vector();");
      e.generateIndexes(out); 
      String indexop = e.generateIndexOp(); 

      addops = addops +
               "  public void add" + nme + "(" +
               nme + " oo) { " + lcnme +
               ".add(oo);"; 
      controllerInterface = controllerInterface + 
               "  public void add" + nme + "(" + nme + " oo);\n  ";
      // controllerInterface += e.interfaceCreateOp(); 
      controllerInterface += e.interfaceKillOp();  
      if (es != null) 
      { addops = addops + " add" + es.getName() + "(oo);"; } 
      // also for each interface of e
      Vector intfs = e.getInterfaces(); 
      for (int j = 0; j < intfs.size(); j++)
      { Entity intf = (Entity) intfs.get(j); 
        addops = addops + " add" + intf.getName() + "(oo);"; 
      }      
      addops = addops + " }\n\n" + indexop;


      String delop = e.buildDeleteOp(assocs,constraints,
                                     entities,types); 
      deleteops = deleteops + delop + "\n\n"; 
      String createop = e.buildCreateOp0(); 
      createops = createops + createop; 
      Vector eassocs = e.getAssociations(); 
      for (int j = 0; j < eassocs.size(); j++) 
      { Association aa = (Association) eassocs.get(j); 
        controllerInterface = controllerInterface + 
                              aa.interfaceSetOperation(e); 
      } 
      Vector eatts = e.getAttributes(); 
      for (int k = 0; k < eatts.size(); k++) 
      { Attribute att = (Attribute) eatts.get(k); 
        controllerInterface = controllerInterface + 
                              att.interfaceSetOperation(e); 
      } 

      Vector eops = e.getOperations(); 
      for (int w = 0; w < eops.size(); w++) 
      { BehaviouralFeature bf = (BehaviouralFeature) eops.get(w); 
        globalops = globalops + bf.getGlobalOperationCode(e,entities,types,
                                                          constraints); 
      } 
    }  // and set and others
    out.println("  private static Controller uniqueInstance; \n\n"); 
    out.println("  private Controller() { } \n\n"); 
    out.println("  public static Controller inst() \n  " + 
                "  { if (uniqueInstance == null) \n" + 
                "    { uniqueInstance = new Controller(); }\n" + 
                "    return uniqueInstance; } \n\n"); 

    if (incr)
    { out.println(getLoadModelDeltaOp()); } 
    else 
    { out.println(getLoadModelOp()); }

    out.println(loadCSVModelOp()); /* Optional */ 

    out.println(getCheckCompletenessOp()); 
    out.println(getSaveModelOp()); 
    out.println(generateLoadXsiOp());  /* Optional */ 
    out.println(generateLoadFromXsiOp());  /* Optional */ 
    out.println(getSaveXSIOp());           /* Optional */ 
    out.println(saveCSVModelOp());         /* Optional */ 

    // out.println(getSaveXSI2Op()); 
    out.println(addops);
    out.println(createops); 
    
    // for each sensor event, look up all constraints
    // with att1 = val1 a poststate of the event,
    // generate their code.
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Vector v = e.sensorOperationsCode(constraints,entities,types);
      v.addAll(e.associationOperationsCode(constraints,entities,types)); 
      for (int j = 0; j < v.size(); j++)
      { String op = (String) v.get(j);
        out.println(op);
      }
    }
    out.println(globalops); 
    out.println(deleteops); 
    out.println("  " + mop + " \n}\n\n");
    controllerInterface = controllerInterface + "}\n"; 
    out3.println(controllerInterface); 
  }

  private void generateControllerJava6(String mop, PrintWriter out)
  { String addops = "\n";
    String deleteops = "\n"; 
    String createops = "\n"; 
    String globalops = "\n";   

    // String controllerInterface = "public interface ControllerInterface\n" + "{\n"; 

    // if (systemName != null && systemName.length() > 0)
    // { out3.println("package " + systemName + ";\n"); } 
    // out3.println("import java.util.Date;"); 
    // out3.println("import java.util.ArrayList;"); 
    // out3.println("import java.util.Map;"); 
    // out3.println("import java.util.HashMap;"); 
    // out3.println("import java.util.HashSet;"); 
    // out3.println("import java.util.Collection;\n");
        
    out.println("public class Controller " +
                "implements SystemTypes");
    out.println("{");
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Entity es = e.getSuperclass(); 
      String nme = e.getName();
      String lcnme = nme.toLowerCase();
      lcnme = lcnme + "s";
      out.println("  ArrayList " + lcnme + " = new ArrayList();");
      e.generateIndexes(out); 
      String indexop = e.generateIndexOpJava6(); 

      addops = addops +
               "  public void add" + nme + "(" + nme + " oo) { " + lcnme +
               ".add(oo);"; 
      // controllerInterface = controllerInterface + 
      //          "  public void add" + nme + "(" + nme + " oo);\n  ";
      // controllerInterface += e.interfaceCreateOp(); 
      // controllerInterface += e.interfaceKillOp();  
      if (es != null) 
      { addops = addops + " add" + es.getName() + "(oo);"; } 
      // also for each interface of e
      Vector intfs = e.getInterfaces(); 
      for (int j = 0; j < intfs.size(); j++)
      { Entity intf = (Entity) intfs.get(j); 
        addops = addops + " add" + intf.getName() + "(oo);"; 
      }      
      addops = addops + " }\n\n" + indexop;

      String delop = e.buildDeleteOpJava6(associations,constraints,
                                     entities,types); 
      deleteops = deleteops + delop + "\n\n"; 
      String createop = e.buildCreateOpJava6(constraints,entities,types); 
      createops = createops + createop; 
      
      /* Vector eassocs = e.getAssociations(); 
      for (int j = 0; j < eassocs.size(); j++) 
      { Association aa = (Association) eassocs.get(j); 
        controllerInterface = controllerInterface + 
                              aa.interfaceSetOperation(e); 
      }  

      Vector eatts = e.getAttributes(); 
      for (int k = 0; k < eatts.size(); k++) 
      { Attribute att = (Attribute) eatts.get(k); 
        controllerInterface = controllerInterface + 
                              att.interfaceSetOperation(e); 
      } */ 

      Vector eops = e.getOperations(); 
      for (int w = 0; w < eops.size(); w++) 
      { BehaviouralFeature bf = (BehaviouralFeature) eops.get(w); 
        globalops = globalops + bf.getGlobalOperationCodeJava6(e,entities,types,
                                                          constraints); 
      } 
    }  // and set and others
    out.println("  private static Controller uniqueInstance; \n\n"); 
    out.println("  private Controller() { } \n\n"); 
    out.println("  public static Controller inst() \n  " + 
                "  { if (uniqueInstance == null) \n" + 
                "    { uniqueInstance = new Controller(); }\n" + 
                "    return uniqueInstance; } \n\n"); 

    out.println(getLoadModelOp()); 
    // out.println(getCheckCompletenessOp()); 
    out.println(getSaveModelOpJava6()); 
    // out.println(getSaveXSIOp()); 
    // out.println(getSaveXSI2Op()); 
    // load, save CSV

    out.println(addops);
    out.println(createops); 
    
    // for each sensor event, look up all constraints
    // with att1 = val1 a poststate of the event,
    // generate their code.
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Vector v = e.sensorOperationsCode(constraints,entities,types);  // Java6?
      v.addAll(e.associationOperationsCodeJava6(constraints,entities,types)); 
      for (int j = 0; j < v.size(); j++)
      { String op = (String) v.get(j);
        out.println(op);
      }
    }
    out.println(globalops); 
    out.println(deleteops); 
    out.println("  " + mop + " \n}\n\n");
    // controllerInterface = controllerInterface + "}\n"; 
    // out3.println(controllerInterface); 

  }

  private void generateControllerJava7(String mop, PrintWriter out)
  { String addops = "\n";
    String deleteops = "\n"; 
    String createops = "\n"; 
    String globalops = "\n";   

    // String controllerInterface = "public interface ControllerInterface\n" + "{\n"; 

    // if (systemName != null && systemName.length() > 0)
    // { out3.println("package " + systemName + ";\n"); } 
    // out3.println("import java.util.Date;"); 
    // out3.println("import java.util.ArrayList;"); 
    // out3.println("import java.util.Map;"); 
    // out3.println("import java.util.HashMap;"); 
    // out3.println("import java.util.HashSet;"); 
    // out3.println("import java.util.Collection;\n");
        
    out.println("public class Controller " +
                "implements SystemTypes");
    out.println("{");
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Entity es = e.getSuperclass(); 
      String nme = e.getName();
      String lcnme = nme.toLowerCase();
      lcnme = lcnme + "s";
      out.println("  ArrayList<" + nme + "> " + lcnme + " = new ArrayList<" + nme + ">();");
      e.generateIndexesJava7(out); 
      String indexop = e.generateIndexOpJava7(); 

      addops = addops +
               "  public void add" + nme + "(" + nme + " oo) { " + lcnme +
               ".add(oo);"; 
      // controllerInterface = controllerInterface + 
      //          "  public void add" + nme + "(" + nme + " oo);\n  ";
      // controllerInterface += e.interfaceCreateOp(); 
      // controllerInterface += e.interfaceKillOp();  
      if (es != null) 
      { addops = addops + " add" + es.getName() + "(oo);"; } 
      // also for each interface of e
      Vector intfs = e.getInterfaces(); 
      for (int j = 0; j < intfs.size(); j++)
      { Entity intf = (Entity) intfs.get(j); 
        addops = addops + " add" + intf.getName() + "(oo);"; 
      }      
      addops = addops + " }\n\n" + indexop;

      String delop = e.buildDeleteOpJava7(associations,constraints,
                                     entities,types); 
      deleteops = deleteops + delop + "\n\n"; 
      String createop = e.buildCreateOpJava7(constraints,entities,types); 
      createops = createops + createop; 
      
      /* Vector eassocs = e.getAssociations(); 
      for (int j = 0; j < eassocs.size(); j++) 
      { Association aa = (Association) eassocs.get(j); 
        controllerInterface = controllerInterface + 
                              aa.interfaceSetOperation(e); 
      }  

      Vector eatts = e.getAttributes(); 
      for (int k = 0; k < eatts.size(); k++) 
      { Attribute att = (Attribute) eatts.get(k); 
        controllerInterface = controllerInterface + 
                              att.interfaceSetOperation(e); 
      } */ 

      Vector eops = e.getOperations(); 
      for (int w = 0; w < eops.size(); w++) 
      { BehaviouralFeature bf = (BehaviouralFeature) eops.get(w); 
        globalops = globalops + bf.getGlobalOperationCodeJava7(e,entities,types,
                                                          constraints); 
      } 
    }  // and set and others
    out.println("  private static Controller uniqueInstance; \n\n"); 
    out.println("  private Controller() { } \n\n"); 
    out.println("  public static Controller inst() \n  " + 
                "  { if (uniqueInstance == null) \n" + 
                "    { uniqueInstance = new Controller(); }\n" + 
                "    return uniqueInstance; } \n\n"); 

    out.println(getLoadModelOp()); 
    // out.println(getCheckCompletenessOp()); 
    out.println(getSaveModelOpJava6()); 
    // out.println(getSaveXSIOp()); 
    // out.println(getSaveXSI2Op()); 
    // load, save CSV, loadXSI

    out.println(addops);
    out.println(createops); 
    
    // for each sensor event, look up all constraints
    // with att1 = val1 a poststate of the event,
    // generate their code.
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Vector v = e.sensorOperationsCodeJava7(constraints,entities,types);
      v.addAll(e.associationOperationsCodeJava7(constraints,entities,types)); 
      for (int j = 0; j < v.size(); j++)
      { String op = (String) v.get(j);
        out.println(op);
      }
    }
    out.println(globalops); 
    out.println(deleteops); 
    out.println("  " + mop + " \n}\n\n");
    // controllerInterface = controllerInterface + "}\n"; 
    // out3.println(controllerInterface); 
  }

  private void generateControllerCSharp(String mop, PrintWriter out)
  { String addops = "\n";
    String deleteops = "\n"; 
    String createops = "\n"; 
    String globalops = "\n";   

    // String controllerInterface = "public interface ControllerInterface\n" + "{\n"; 
        
    out.println("class Controller");
    out.println("{");
    out.println("  Hashtable objectmap = new Hashtable();"); 
    out.println("  Hashtable classmap = new Hashtable();"); 

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Entity es = e.getSuperclass(); 
      String nme = e.getName();
      String lcnme = nme.toLowerCase();
      lcnme = lcnme + "_s";
      out.println("  ArrayList " + lcnme + " = new ArrayList();");
      e.generateCSharpIndexes(out); 
      String indexop = e.generateCSharpIndexOp(); 

      addops = addops +
               "  public void add" + nme + "(" +
               nme + " oo) { " + lcnme + ".Add(oo);"; 
      // controllerInterface = controllerInterface + 
      //         "  public void add" + nme + "(" + nme + " oo);\n  ";
      // controllerInterface += e.interfaceCreateOp(); 
      // controllerInterface += e.interfaceKillOp();  
      if (es != null) 
      { addops = addops + " add" + es.getName() + "(oo);"; } 
      // also for each interface of e
      Vector intfs = e.getInterfaces(); 
      for (int j = 0; j < intfs.size(); j++)
      { Entity intf = (Entity) intfs.get(j); 
        addops = addops + " add" + intf.getName() + "(oo);"; 
      }      
      addops = addops + " }\n\n" + indexop + 
      " public ArrayList get" + lcnme + "() { return " + lcnme + "; }\n\n";

      String delop = e.buildDeleteOpCSharp(associations,constraints,
                                           entities,types); 
      deleteops = deleteops + delop + "\n\n"; 
      String createop = e.buildCreateOpCSharp(constraints,entities,types); 
      createops = createops + createop; 
      Vector eassocs = e.getAssociations(); 
      /* for (int j = 0; j < eassocs.size(); j++) 
      { Association aa = (Association) eassocs.get(j); 
        controllerInterface = controllerInterface + 
                              aa.interfaceSetOperation(e); 
      } */ 
      Vector eatts = e.getAttributes(); 
      /* for (int k = 0; k < eatts.size(); k++) 
      { Attribute att = (Attribute) eatts.get(k); 
        controllerInterface = controllerInterface + 
                              att.interfaceSetOperation(e); 
      } */ 

      Vector eops = e.getOperations(); 
      for (int w = 0; w < eops.size(); w++) 
      { BehaviouralFeature bf = (BehaviouralFeature) eops.get(w); 
        globalops = globalops + bf.getGlobalOperationCodeCSharp(e,entities,types,
                                                          constraints); 
      } 
    }  // and set and others
    out.println("  private static Controller uniqueInstance; \n\n"); 
    out.println("  private Controller() { } \n\n"); 
    out.println("  public static Controller inst() \n  " + 
                "  { if (uniqueInstance == null) \n" + 
                "    { uniqueInstance = new Controller(); }\n" + 
                "    return uniqueInstance; } \n\n"); 

    out.println(getLoadModelOpCSharp()); 
    // out.println(getCheckCompletenessOp()); 
    out.println(getSaveModelOpCSharp()); 
    // out.println(getSaveXSIOp()); 
    // out.println(getSaveXSI2Op()); 
    // load, save CSV, loadXSI
    out.println(addops);
    out.println(createops); 
    
    // for each sensor event, look up all constraints
    // with att1 = val1 a poststate of the event,
    // generate their code.
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Vector v = e.sensorOperationsCodeCSharp(constraints,entities,types);
      v.addAll(e.associationOperationsCodeCSharp(constraints,entities,types)); 
      for (int j = 0; j < v.size(); j++)
      { String op = (String) v.get(j);
        out.println(op);
      }
    }
    out.println(globalops); 
    out.println(deleteops); 
    out.println("  " + mop + " \n}\n\n");
    // controllerInterface = controllerInterface + "}\n"; 
    // out3.println(controllerInterface); 
  }

  public String getLoadModelOpCSharp()
  { String res = "  public void loadModel()\n" + 
      "  { char[] delims = {' ', '.'};\n" + 
      "    StreamReader str = new StreamReader(\"in.txt\");\n" +
      "    string line = \"\";\n" +
      "    try\n" +
      "    { while (true)\n" +
      "      { line = str.ReadLine();\n" +
      "        if (line == null) { return; }\n" +
      "        string[] words = line.Split(delims);\n" +
      "        if (words.Length == 3 && words[1].Equals(\":\"))  // a : A\n" +
      "        { addObjectToClass(words[0], words[2]); }\n" +
      "        else if (words.Length == 4 && words[1].Equals(\":\")) // a : b.role\n" +
      "        { addObjectToRole(words[2], words[0], words[3]); }\n" +
      "        else if (words.Length >= 4 && words[2].Equals(\"=\"))  // a.f = val\n" +
      "        { int eqind = line.IndexOf(\"=\");\n" +
      "          if (eqind < 0) { continue; }\n" +
      "          string value = line.Substring(eqind+1,line.Length-eqind-1);\n" +
      "          setObjectFeatureValue(words[0], words[1], value.Trim());\n" +
      "        }\n" +
      "      }\n" +
      "    }  catch(Exception e) { return; }\n" +
      "  }\n\n"; 

    res = res + "  public void addObjectToClass(string a, string c)\n" +
                "  {";

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.getCSharpAddObjOp();
    }
    res = res + "  }\n\n"; 

    res = res + "  public void addObjectToRole(string a, string b, string role)\n" +
                "  {";

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.getCSharpAddRoleOp();
    }
    res = res + " }\n\n";

    res = res + "  public void setObjectFeatureValue(string a, string f, string val)\n" +
                "  {";

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.getCSharpSetFeatureOp();
    }

    return res + " }\n\n";
  } 

  public String getLoadModelOpCPP()
  { String res = "  void Controller::loadModel()\n" + 
      "  { ifstream infle(\"in.txt\");\n" +
      "    if (infle.fail()) { cout << \"No input file!\" << endl; return; }\n" + 
      "    string* str = new string(\"\");\n" + 
      "    vector<string>* res = new vector<string>();\n" +    
      "    while (!infle.eof())\n" + 
      "    { std::getline(infle,*str);\n" +      
      "      vector<string>* words = UmlRsdsLib<string>::tokenise(res,*str);\n" + 
      "      if (words->size() == 3 && (*words)[1] == \":\")  // a : A\n" +
      "      { addObjectToClass((*words)[0], (*words)[2]); }\n" +
      "      else if (words->size() == 4 && (*words)[1] == \":\") // a : b.role\n" +
      "      { addObjectToRole((*words)[2], (*words)[0], (*words)[3]); }\n" +
      "      else if (words->size() >= 4 && (*words)[2] == \"=\")  // a.f = val\n" +
      "      { int eqind = str->find(\"=\");\n" +
      "        if (eqind < 0) { continue; }\n" +
      "        int f1ind = str->find_first_of(\"\\\"\");\n" +  
      "        int f2ind = str->find_last_of(\"\\\"\");\n" + 
      "        string value;\n" + 
      "        if (f1ind != string::npos && f2ind != string::npos)\n" +  
      "        { value = str->substr(f1ind, f2ind-f1ind+1); }\n" +
      "        else if (words->size() == 4)\n" + 
      "        { value = (*words)[3]; }\n" +
      "        else if (words->size() == 5)\n" + 
      "        { value = (*words)[3] + \".\" + (*words)[4]; }\n" +  
      "        setObjectFeatureValue((*words)[0], (*words)[1], value);\n" +
      "      }\n" +
      "      res->clear();\n" + 
      "    }\n" + 
      "  }\n\n"; 

    res = res + "  void Controller::addObjectToClass(string a, string c)\n" +
                "  {";

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.getCPPAddObjOp();
    }
    res = res + "  }\n\n"; 

    res = res + "  void Controller::addObjectToRole(string a, string b, string role)\n" +
                "  {";

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.getCPPAddRoleOp();
    }
    res = res + " }\n\n";

    res = res + "  void Controller::setObjectFeatureValue(string a, string f, string val)\n" +
                "  {";

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.getCPPSetFeatureOp();
    }

    return res + " }\n\n";
  } 

  public String getLoadModelOpCPPDeclarations()
  { String res = "  void loadModel();\n" + 
      "  void addObjectToClass(string a, string c);\n" +
      "  void addObjectToRole(string a, string b, string role);\n" +
      "  void setObjectFeatureValue(string a, string f, string val);\n" + 
      "  void saveModel(string f);\n"; 
    return res;
  } 


  private void generateControllerCPP(String mop, PrintWriter out, PrintWriter out2)
  { String addops = "\n";
    String deleteops = "\n"; 
    String createops = "\n"; 
    String globalops = "\n";   
    String initstring = ""; 
    String destructor = "  ~Controller() { \n"; 
        
    out.println("class Controller");
    out.println("{ private: ");
    out.println("    map<string,void*> objectmap;"); 
    out.println("    map<string,string> classmap;"); 

    Vector declarations = new Vector(); // declarations of controller ops. 

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Entity es = e.getSuperclass(); 
      String nme = e.getName();
      String lcnme = nme.toLowerCase();
      lcnme = lcnme + "_s";
      initstring = initstring + "  " + lcnme + " = new vector<" + nme + "*>();\n";
      destructor = destructor + "  delete " + lcnme + ";\n"; 

      out.println("    vector<" + nme + "*>* " + lcnme + ";");
      e.generateIndexesCPP(out); 
      String indexop = e.generateIndexOpCPP(); 

      addops = addops +
               "  void add" + nme + "(" +
               nme + "* _oo) { " + lcnme + "->push_back(_oo);"; 
      // controllerInterface = controllerInterface + 
      //         "  public void add" + nme + "(" + nme + " oo);\n  ";
      // controllerInterface += e.interfaceCreateOp(); 
      // controllerInterface += e.interfaceKillOp();  
      if (es != null) 
      { addops = addops + " add" + es.getName() + "(_oo);"; } 
      // also for each interface of e
      Vector intfs = e.getInterfaces(); 
      for (int j = 0; j < intfs.size(); j++)
      { Entity intf = (Entity) intfs.get(j); 
        addops = addops + " add" + intf.getName() + "(_oo);"; 
      }      
      addops = addops + " }\n\n" + indexop;

      String delop = e.buildDeleteOpCPP(associations,constraints,
                                           entities,types); 
      deleteops = deleteops + delop + "\n\n"; 

      String createop = e.buildCreateOpCPP(constraints,entities,types); 
      createops = createops + createop + 
                  "  vector<" + nme + "*>* get" + lcnme + "() { return " + lcnme + "; }\n\n"; 
      Vector eassocs = e.getAssociations(); 
      Vector eatts = e.getAttributes(); 
      
      Vector eops = e.getOperations(); 
      for (int w = 0; w < eops.size(); w++) 
      { BehaviouralFeature bf = (BehaviouralFeature) eops.get(w); 
        globalops = globalops + bf.getGlobalOperationCodeCPP(e,entities,types,
                                                             constraints,declarations); 
      } 
    }  // and set and others

    out.println("");  
    out.println(" public: ");
    out.println("");  
    out.println("  static Controller* inst; \n\n"); 
    out.println("  Controller() { " + initstring + "  } \n\n"); 



    out.println(getLoadModelOpCPPDeclarations()); 
    // out.println(getCheckCompletenessOp()); 
    // out.println(getSaveXSIOp()); 
    // out.println(getSaveXSI2Op()); 
    out.println(addops);
    out.println(createops); 
    
    // for each sensor event, look up all constraints
    // with att1 = val1 a poststate of the event,
    // generate their code.
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      if (e.hasStereotype("external") || e.hasStereotype("externalApp")) { continue; } 

      Vector v = e.sensorOperationsCodeCPP(constraints,entities,types);
      v.addAll(e.associationOperationsCodeCPP(constraints,entities,types)); 
      for (int j = 0; j < v.size(); j++)
      { String op = (String) v.get(j);
        out.println(op);
      }
    }

    for (int h = 0; h < declarations.size(); h++) 
    { String declh = (String) declarations.get(h); 
      out.println(declh); 
    } 
    out.println(); 
    out2.println(getLoadModelOpCPP()); 
    out2.println(getSaveModelOpCPP()); 
    out2.println(globalops); 

    out.println(deleteops); 
    out.println(); 
    out.println(destructor + "  }\n\n"); 
    out.println("  " + mop + " \n};\n\n");

    out.println(BSystemTypes.getForAllOps()); 
    out.println(BSystemTypes.getSelectOps());
    out.println(BSystemTypes.getRejectOps());
    out.println(BSystemTypes.getExistsOps()); 
    out.println(BSystemTypes.getExists1Ops()); 
    out.println(BSystemTypes.getCollectOps()); 

    // controllerInterface = controllerInterface + "}\n"; 
    // out3.println(controllerInterface); 
  }

  public String getSaveModelOp()
  { String res = "  public void saveModel(String file)\n";
    res =  res + "  { File outfile = new File(file); \n" + 
                 "    PrintWriter out; \n" + 
                 "    try { out = new PrintWriter(new BufferedWriter(new FileWriter(outfile))); }\n" + 
                 "    catch (Exception e) { return; } \n";  

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel1();
    }

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel2();
    }

    return res + "    out.close(); \n" + 
                 "  }\n\n";
  }

  public String getSaveModelOpJava6()
  { String res = "  public void saveModel(String file)\n";
    res =  res + "  { File outfile = new File(file); \n" + 
                 "    PrintWriter out; \n" + 
                 "    try { out = new PrintWriter(new BufferedWriter(new FileWriter(outfile))); }\n" + 
                 "    catch (Exception e) { return; } \n";  

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel1();
    }

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel2Java6();
    }

    return res + "    out.close(); \n" + 
                 "  }\n\n";
  }

  public String getSaveModelOpCSharp()
  { String res = "  public void saveModel(string f)\n";
    res =  res + "  { StreamWriter outfile = new StreamWriter(f); \n"; 
                 
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel1CSharp();
    }

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel2CSharp();
    }

    return res + "    outfile.Close(); \n" + 
                 "  }\n\n";
  }

  public String getSaveModelOpCPP()
  { String res = "  void Controller::saveModel(string f)\n";
    res =  res + "  { ofstream outfile(f.c_str()); \n"; 
                 
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel1CPP();
    }

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.generateSaveModel2CPP();
    }

    return res + "\n  }\n\n";
  }

  public String getCheckCompletenessOp()
  { String res = "  public void checkCompleteness()\n";
    res =  res + "  { ";  
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.checkCompletenessOp();
    }
    for (int i = 0; i < associations.size(); i++)
    { Association a = (Association) associations.get(i);
      res = res + a.checkCompletenessOp();
    }
    return res + "  }\n\n";
  }

  public String getSaveXSIOp()
  { String res = "  public void saveXSI(String file)\n";
    res =  res + "  { File outfile = new File(file); \n" + 
                 "    PrintWriter out; \n" + 
                 "    try { out = new PrintWriter(new BufferedWriter(new FileWriter(outfile))); }\n" + 
                 "    catch (Exception e) { return; } \n";  
    res = res +  "    out.println(\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\");\n" + 
                 "    out.println(\"<UMLRSDS:model xmi:version=\\\"2.0\\\" xmlns:xmi=\\\"http://www.omg.org/XMI\\\">\");\n"; 

    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.xsiSaveModel();
    }
    return res + "    out.println(\"</UMLRSDS:model>\");\n" + 
                 "    out.close(); \n" + 
                 "  }\n\n";
  }

  public String getSaveXSI2Op()
  { String res = "  public void saveXSI2(String file)\n";
    res =  res + "  { File outfile = new File(file); \n" + 
                 "    PrintWriter out; \n" + 
                 "    try { out = new PrintWriter(new BufferedWriter(new FileWriter(outfile))); }\n" + 
                 "    catch (Exception e) { return; } \n";  
    res = res +  "    out.println(\"<?xml version=\\\"1.0\\\" encoding=\\\"ASCII\\\"?>\");\n" + 
                 "    out.println(\"<UMLRSDS:model xmi:version=\\\"2.0\\\" xmlns:xmi=\\\"http://www.omg.org/XMI\\\" xmlns:xmi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"http://UMLRSDS UMLRSDS.ecore\\\">\");\n"; 

    res = res +  "    List eRELATIONS = new Vector();\n" + 
                 "    List eELEMENTS = new Vector();\n";   
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.xsi2SettupModel();
    }
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.xsi2SaveModel();
    }
    return res + "    out.println(\"</UMLRSDS:model>\");\n" + 
                 "    out.close(); \n" + 
                 "  }\n\n";
  }

  private void generateLookupOps()  // Put in Java Controller
  { for (int i = 0; i < entities.size(); i++)
    { Entity ent = (Entity) entities.get(i);
      String ename = ent.getName();
      String es = ename.toLowerCase() + "s";
      String ex = ename.toLowerCase() + "x";
      String params = ent.getLookupParameters();
      String pvals = ent.getLookupParamValues();
      System.out.println("  private static " +       // public? 
        ename + " lookup" + ename + "(" + params + ")");
      System.out.println("  { for (int i = 0; " +
        "i < " + es + ".size(); i++)");
      System.out.println("    { " + ename + "  " +
        ex + " = (" + ename + ") " + es +
        ".get(i);");
      System.out.println("      if (" + ex + ".equalFields(" +
        pvals + "))");
      System.out.println("      { return " + ex + "; }");
      System.out.println("    }");
      System.out.println("    return null;");
      System.out.println("  }");
    }
  }

  public void generateRESTWebService()
  { File file = new File("output/ControllerBean.java");
    try
    { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
      OperationDescription.createControllerBean(useCases,entities,out); 
      out.close(); 
    } 
    catch (IOException e) 
    { System.out.println("Error saving data"); } 

    for (int i = 0; i < useCases.size(); i++)
    { Object obj = useCases.get(i); 
      if (obj instanceof UseCase) 
      { UseCase uc = (UseCase) obj; 
        String nme = uc.getName(); 
        File odjsp = new File("output/" + nme + ".jsp"); 
        try
        { PrintWriter jspout = new PrintWriter(
                               new BufferedWriter(
                                new FileWriter(odjsp)));
          jspout.println(uc.getJsp(systemName)); 
          jspout.close(); 
        } catch (Exception e) { } 

        File odhtml = new File("output/" + nme + ".html"); 
        try
        { PrintWriter odhout = new PrintWriter(
                                new BufferedWriter(
                                  new FileWriter(odhtml)));
          odhout.println(uc.getInputPage(systemName));
          odhout.close(); 
        } catch (Exception e) { } 
      } 
    } 
  } 


  public void generateSOAPWebService()
  { File file = new File("output/ControllerWebBean.java");
    try
    { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
      OperationDescription.createWebServiceBean(useCases,entities,out); 
      out.close(); 
    } 
    catch (IOException e) 
    { System.out.println("Error saving data"); } 
  } 


  public void saveDataToFile()
  { Vector saved = new Vector(); 
    File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Save System Data");
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { File file = fc.getSelectedFile();
      try
      { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
        Vector locals = saveComponents(out); 
        locals.addAll(constraints); 
        Expression.saveOperators(out); 
        for (int i = 0; i < locals.size(); i++) 
        { Constraint inv = (Constraint) locals.get(i); 
          inv.saveData(out); 
        } 
        for (int p = 0; p < useCases.size(); p++) 
        { ModelElement uc = (ModelElement) useCases.get(p); 
          if (uc instanceof UseCase) 
          { ((UseCase) uc).saveData(out,saved); } 
          else 
          { uc.saveData(out); }  
        } 
        out.close(); 
      }
      catch (IOException e) 
      { System.out.println("Error saving data"); } 
    }
  }

  public void saveDataToFile(String f)
  { File file = new File("output/" + f);
    Vector saved = new Vector(); 

    try
    { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
      Vector locals = saveComponents(out); 
      locals.addAll(constraints); 
      Expression.saveOperators(out); 
      for (int i = 0; i < locals.size(); i++) 
      { Constraint inv = (Constraint) locals.get(i); 
        inv.saveData(out); 
      } 
      for (int p = 0; p < useCases.size(); p++) 
      { ModelElement uc = (ModelElement) useCases.get(p); 
        if (uc instanceof UseCase) 
        { ((UseCase) uc).saveData(out,saved); } 
        else 
        { uc.saveData(out); }  
      } 
      out.close(); 
    }
    catch (IOException e) 
    { System.out.println("Error saving data"); } 
  }

  public void saveEMFToFile()
  { File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Save Model as EMF file");
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { File file = fc.getSelectedFile();
      try
      { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
        saveEMF(out); 
        out.close(); 
      }
      catch (IOException e) 
      { System.out.println("Error saving EMF"); } 
    }
  }

  public void saveKM3ToFile()
  { File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Save Model as KM3 file");
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { File file = fc.getSelectedFile();
      try
      { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
        saveKM3(out); 
        out.close(); 
      }
      catch (IOException e) 
      { System.out.println("Error saving KM3"); } 
    }
  }

  public void saveSimpleKM3ToFile()
  { File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Save Model as KM3 file");
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { File file = fc.getSelectedFile();
      try
      { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
        saveSimpleKM3(out); 
        out.close(); 
      }
      catch (IOException e) 
      { System.out.println("Error saving KM3"); } 
    }
  }


  public void saveEcoreToFile()
  { File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Save Model as Ecore file");
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { File file = fc.getSelectedFile();
      try
      { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
        saveEcore(out); 
        out.close(); 
      }
      catch (IOException e) 
      { System.out.println("Error saving Ecore"); } 
    }
  }


  public void saveInterfaceDescription(String f)
  { // The signatures of the use cases are saved
    File file = new File(f);
    try
    { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));

      for (int i = 0; i < useCases.size(); i++) 
      { ModelElement uc = (ModelElement) useCases.get(i); 

        if (uc instanceof UseCase)
        { ((UseCase) uc).saveInterfaceDescription(out); }  
      }  
      out.close(); 
    }
    catch (IOException e) 
    { System.out.println("Error saving model"); } 
  }
   
    
  // Saves the class diagram as an *instance model* of UML-RSDS metamodel
  public void saveModelToFile(String f)
  { File file = new File(f);
    // JFileChooser fc = new JFileChooser();
    // fc.setCurrentDirectory(startingpoint);
    // fc.setDialogTitle("Save Model as text file");
    // int returnVal = fc.showSaveDialog(this);
    // if (returnVal == JFileChooser.APPROVE_OPTION)
    { // File file = fc.getSelectedFile();
      try
      { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));

        Vector visualentities = saveModel(out); 
        // locals.addAll(constraints); 
        // for (int i = 0; i < locals.size(); i++) 
        // { Constraint inv = (Constraint) locals.get(i); 
        //   inv.saveData(out); 
        // } 

        saveAdditionalOperations(visualentities, out); 

        Vector saved = new Vector(); 
        for (int p = 0; p < useCases.size(); p++) 
        { ModelElement uc = (ModelElement) useCases.get(p); 

          if (uc instanceof OperationDescription)
          { ((OperationDescription) uc).saveModelData(out,saved); } 
          else if (uc instanceof UseCase)
          { ((UseCase) uc).saveModelData(out,saved,entities,types); }  
        } 
        out.close(); 
      }
      catch (IOException e) 
      { System.out.println("Error saving model"); } 
    }
  }

  public void saveUSEDataToFile(String f)
  { File file = new File("output/" + f);
    Vector locals = new Vector(); 

    try
    { PrintWriter out =
          new PrintWriter(
            new BufferedWriter(new FileWriter(file)));
      out.println("model " + systemName); 
      for (int i = 0; i < associations.size(); i++) 
      { Association ast = (Association) associations.get(i); 
        out.println(ast.saveAsUSEData()); 
      } 
      for (int p = 0; p < entities.size(); p++) 
      { Entity ent = (Entity) entities.get(p); 
        out.println(ent.saveAsUSEData()); 
        locals.addAll(ent.getInvariants()); 
      } 

      if (locals.size() > 0)
      { out.println("constraints\n\n"); } 

      for (int q = 0; q < locals.size(); q++)
      { Constraint cc = (Constraint) locals.get(q); 
        out.println(cc.saveAsUSEData()); 
      } 
      out.close(); 
    }
    catch (IOException e) 
    { System.out.println("Error saving data"); } 
  }


 
  public VisualData getVisualOf(ModelElement me)
  { VisualData res = null; 
    for (int i = 0; i < visuals.size(); i++)
    { VisualData vd = (VisualData) visuals.get(i); 
      ModelElement me2 = (ModelElement) vd.getModelElement(); 
      if (me2 == me)
      { return vd; } 
    } 
    return res; 
  } 

  public void removeVisual(VisualData vd)
  { visuals.remove(vd); } 

  public void removeVisual(String nme)
  { for (int i = 0; i < visuals.size(); i++)
    { VisualData vd = (VisualData) visuals.get(i); 
      if (vd.getName().equals(nme))
      { visuals.remove(vd); 
        return; 
      }
    } 
  }  

  private Vector saveComponents(PrintWriter out)
  { Vector locals = new Vector(); 
    
    for (int i = 0; i < visuals.size(); i++)
    { VisualData vd = (VisualData) visuals.get(i);
      if (vd instanceof OvalData) { continue; }   // ignore it  
      ModelElement me = (ModelElement) vd.getModelElement(); 

      if (vd instanceof RectData) // Entity or Type
      { if (me instanceof Entity) 
        { out.println("Entity:"); } 
        else 
        { out.println("Type:"); } 
        RectData rd = (RectData) vd;
        out.println(me.getName() + " " + rd.getx() + " " + rd.gety());
        // and all its attributes 
      } 
      else if (vd instanceof LineData && me != null) // Association or Generalisation or Flow
      { LineData ld = (LineData) vd;
        
        if (me instanceof Association)
        { out.println("Association:"); 
          Association ast = (Association) me; 
          out.print(ast.getEntity1() + " " + 
                      ast.getEntity2() + " " + 
                      ast.getCard1() + " " + ld.xstart + " " + ld.ystart + " " +
                      ld.xend + " " + ld.yend + " " + ast.getCard2() + " " + 
                      ast.getRole2() + " " + ast.getRole1() + " "); 
          out.println(saveWaypoints(ld)); 
        } // should also save waypoints
        else if (me instanceof Generalisation)
        { out.println("Generalisation:");
          Generalisation ast = (Generalisation) me;
          out.println(ast.getAncestor() + " " +
                      ast.getDescendent() + " " +
                      ld.xstart + " " + ld.ystart + " " +
                      ld.xend + " " + ld.yend);  
          out.println(saveWaypoints(ld));
        }
      } 
      if (me != null) 
      { out.println(me.saveData()); }  
      out.println(); 
      if (me != null && me instanceof Entity) 
      { Entity ent = (Entity) me; 
        locals.addAll(ent.getInvariants());
        ent.saveAllOps(out);  
      } 
    } 
    // for each entity, accumulate operations, then save all of these. 

    for (int i = 0; i < activities.size(); i++) 
    { Behaviour bh = (Behaviour) activities.get(i); 
      bh.saveData(out); 
    }       
    return locals; 
  } 

  String saveWaypoints(LineData ld)
  { Vector wps = ld.getWaypoints(); 
    String res = ""; 
    for (int i = 0; i < wps.size(); i++) 
    { LinePoint pt = (LinePoint) wps.get(i); 
      res = res + pt.x + " " + pt.y + " "; 
    } 
    return res; 
  } 

  private void saveBasicTypes(PrintWriter out) 
  { out.println("Integer : PrimitiveType"); 
    out.println("Integer.name = \"int\""); 
    out.println("Integer.typeId = \"-5\""); 
    out.println("Boolean : PrimitiveType"); 
    out.println("Boolean.name = \"boolean\""); 
    out.println("Boolean.typeId = \"-1\""); 
    out.println("Real : PrimitiveType"); 
    out.println("Real.name = \"double\"");
    out.println("Real.typeId = \"-2\""); 
    out.println("Long : PrimitiveType"); 
    out.println("Long.name = \"long\""); 
    out.println("Long.typeId = \"-3\""); 
    
    out.println("String : PrimitiveType"); 
    out.println("String.name = \"String\""); 
    out.println("String.typeId = \"-4\""); 
    out.println("void : PrimitiveType"); 
    out.println("void.name = \"void\""); 
    out.println("void.typeId = \"void\""); 
    // out.println("SetType : CollectionType"); 
    // out.println("SetType.name = \"Set\""); 
    // out.println("SequenceType : CollectionType"); 
    // out.println("SequenceType.name = \"Sequence\""); 
  }     

  private Vector saveModel(PrintWriter out)
  { Vector locals = new Vector(); 
    Vector realentities = new Vector(); 

    saveBasicTypes(out); 
    
    for (int i = 0; i < visuals.size(); i++)
    { VisualData vd = (VisualData) visuals.get(i); 
      ModelElement me = (ModelElement) vd.getModelElement(); 
      if (vd instanceof RectData) // Entity or Type
      { me.asTextModel(out); }  
      else if (vd instanceof LineData) // Association or Generalisation
      { LineData ld = (LineData) vd;
        if (me instanceof Association)
        { // out.println("Association:"); 
          Association ast = (Association) me; 
          ast.saveModelData(out); 
          // out.print(ast.getEntity1() + " " + 
          //            ast.getEntity2() + " " + 
          //            ast.getCard1() + " " + ld.xstart + " " + ld.ystart + " " +
          //            ld.xend + " " + ld.yend + " " + ast.getCard2() + " " + 
          //            ast.getRole2() + " " + ast.getRole1() + " "); 
          // out.println(saveWaypoints(ld)); 
        } // should also save waypoints
        else if (me instanceof Generalisation)
        { // out.println("Generalisation:");
          Generalisation ast = (Generalisation) me;
          ast.asTextModel(out); 
        }
      } 
      out.println(); 

      if (me instanceof Entity) 
      { Entity ent = (Entity) me; 
        realentities.add(ent); 
        // locals.addAll(ent.getAttributes());
        // ent.saveAllOps(out);  
      } 
    } 
    // for each entity, accumulate operations, then save all of these. 

    for (int i = 0; i < realentities.size(); i++) 
    { Entity e = (Entity) realentities.get(i);
      e.asTextModel2(out,entities,types); 
    } 

    return realentities; 
  } 

  private void saveAdditionalOperations(Vector realentities, PrintWriter out)
  { for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 

      if (ast.getCard2() != ModelElement.ONE) 
      { BehaviouralFeature bf1 = ast.designAddOperation();
        BehaviouralFeature bf2 = ast.designRemoveOperation(); 
        bf1.typeCheck(types, entities); 
        bf2.typeCheck(types, entities); 
        System.out.println(bf1.display()); 
        System.out.println(bf2.display());  
        bf1.saveModelData(out, bf1.getEntity(), entities, types); 
        bf2.saveModelData(out, bf2.getEntity(), entities, types); 
      } 

      if (ast.getRole1() != null && ast.getRole1().length() > 0)
      { Association opp = ast.getOpposite(); 
        if (ast.getCard1() != ModelElement.ONE)
        { BehaviouralFeature oppbf1 = opp.designAddOperation();
          BehaviouralFeature oppbf2 = opp.designRemoveOperation(); 
          oppbf1.typeCheck(types, entities); 
          oppbf2.typeCheck(types, entities); 
          System.out.println(oppbf1.display()); 
          System.out.println(oppbf2.display());  
          oppbf1.saveModelData(out, oppbf1.getEntity(), entities, types); 
          oppbf2.saveModelData(out, oppbf2.getEntity(), entities, types);
        }  

        BehaviouralFeature bf3 = ast.designSetOperation(); 
        bf3.typeCheck(types, entities); 
        System.out.println(bf3.display()); 
        bf3.saveModelData(out, bf3.getEntity(), entities, types);

        BehaviouralFeature bf4 = opp.designSetOperation(); 
        bf4.typeCheck(types, entities); 
        System.out.println(bf4.display()); 
        bf4.saveModelData(out, bf4.getEntity(), entities, types);
      }  
    }

    for (int j = 0; j < realentities.size(); j++) 
    { Entity ent = (Entity) realentities.get(j); 
      BehaviouralFeature bf = ent.designKillOp(associations); 
      bf.typeCheck(types, entities); 
      bf.saveModelData(out, bf.getEntity(), entities, types);
      System.out.println(bf.display());  

      if (ent.isAbstract())
      { BehaviouralFeature abskill = ent.designAbstractKillOp(); 
        abskill.typeCheck(types, entities); 
        abskill.saveModelData(out, abskill.getEntity(), entities, types);
        System.out.println(abskill.display());  
      } 
    } 
  }  

  private void saveEMF(PrintWriter out)
  { out.println("@namespace(uri=\"" + systemName + "\", prefix=\"" + systemName + "\")");
    out.println("package " + systemName + ";"); 
 
    for (int i = 0; i < entities.size(); i++)
    { Entity ee = (Entity) entities.get(i); 
      ee.saveEMF(out); 
    } 
    out.println(); 
  } // and associations

  private void saveKM3(PrintWriter out)
  { if (systemName != null && systemName.length() > 0) 
    { out.println("package " + systemName + " { "); } 
    else 
    { out.println("package app {"); } 
 
    for (int i = 0; i < types.size(); i++)
    { Type tt = (Type) types.get(i); 
      tt.saveKM3(out); 
    } 

    for (int i = 0; i < entities.size(); i++)
    { Entity ee = (Entity) entities.get(i); 
      ee.saveKM3(out); 
    } 

    for (int i = 0; i < useCases.size(); i++)
    { Object ee = useCases.get(i);
      if (ee instanceof UseCase) 
      { UseCase uc = (UseCase) ee; 
        Vector saved = new Vector(); 
        uc.saveKM3(out, saved);
      }  
    } 

    out.println("}"); 
  } // and use cases

  private void saveSimpleKM3(PrintWriter out)
  { 
    for (int i = 0; i < entities.size(); i++)
    { Entity ee = (Entity) entities.get(i); 
      ee.saveSimpleKM3(out); 
    } 

    out.println(""); 
  } // and use cases

  private void saveEcore(PrintWriter out)
  { // String res = "<eClassifiers xsi:type=\"ecore:EClass\" name=\"Root\">\n";
 
    out.println("<?xml version = \"1.0\" encoding = \"UTF-8\"?>");
    out.println("<ecore:EPackage xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\"  xmlns:xmi=\"http://www.w3.org/2001/XMLSchema-instance\""); 
    out.println("   xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\" name=\"UMLRSDS\" nsURI=\"platform:/output/UMLRSDS.ecore\" nsPrefix=\"UMLRSDS\">"); 
 
    for (int i = 0; i < entities.size(); i++)
    { Entity ee = (Entity) entities.get(i); 
      ee.saveEcore(out);
      // String enme = ee.getName(); 
      // String es = enme.toLowerCase() + "s"; 
      // res = res + "  <eStructuralFeatures xsi:type=\"ecore:EReference\""; 
      // res = res + " name=\"" + es + "\" upperBound=\"-1\" eType=\"#//" + enme +
      //       "\" containment=\"true\"/>\n";   
    } 
    // out.println(res + "</eClassifiers>"); 
    for (int j = 0; j < types.size(); j++) 
    { Type tt = (Type) types.get(j); 
      tt.saveEcore(out); 
    } 

    Vector leafs = Entity.allLeafClasses(entities); 

    out.println("<eClassifiers xsi:type=\"ecore:EClass\" name=\"model\">");
    for (int i = 0; i < leafs.size(); i++) 
    { Entity e = (Entity) leafs.get(i); 
      String ename = e.getName();  
      String erefname = ename.toLowerCase() + "s";  
      out.println(" <eStructuralFeatures xsi:type=\"ecore:EReference\" name=\"" + erefname + 
                  "\" eType=\"#//" + ename + "\" upperBound=\"-1\" containment=\"true\"/>"); 
    } 
    out.println("</eClassifiers>");  
    out.println("</ecore:EPackage>"); 
  } 

  public void loadATL()
  { 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ee = (Entity) entities.get(i); 
      if (ee.isRoot() && !ee.isInterface())
      { ee.addPrimaryKey("$id"); } 
    } 

    Compiler2 c = new Compiler2(); 
    BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/mm.atl");  /* default */ 

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException _e)
    { System.out.println("File not found: " + file);
      return; 
    }

    String atlstring = ""; 
    int noflines = 0; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException _ex)
      { System.out.println("Reading ATL file failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else if (s.startsWith("--")) { } 
      else 
      { atlstring = atlstring + s + " "; } 
      noflines++; 
    }
    c.nospacelexicalanalysis(atlstring); 

    System.out.println(); 
    
        //  c.displaylexs(); 
    System.out.println(); 

    ATLModule mr = c.parseATL(entities,types); 
    if (mr == null) 
    { System.err.println("Invalid syntax"); 
      return; 
    } 
    mr.typeCheck(types,entities); 

    System.out.println(); 
    System.out.println(); 
    System.out.println(); 
    System.err.println("*** " + noflines + " Lines"); 

    int cn = c.checkclones(types,entities);
    System.err.println("*** " + cn + " Clones of size 10 tokens or more");  

    int atlcomplexity = mr.complexity(); 
    System.out.println("*** Syntactic complexity of module " + mr.name + " is " + atlcomplexity); 
    System.out.println(); 
    int atlcc = mr.cyclomaticComplexity(); 
    int atlepl = mr.epl(); 
    System.out.println("*** " + atlepl + " rules/helpers have excessive parameter length"); 
    int atluex = mr.uex(); 
    System.out.println("*** UEX = " + atluex); 
    Map mm = mr.getCallGraph(); 
    System.out.println("*** Call graph of module " + mr.getName() + " is: "); 

    Map mm1 = mm.removeDuplicates(); 

    System.out.println(mm1); 

    Map tc = new Map(); 

    tc.elements = Map.transitiveClosure(mm1.elements);
    int cgsize = mm1.elements.size(); 
    System.out.println("*** Size of call graph = " + cgsize);  

    int nos = mr.nops(); 
    int nrs = mr.nrules(); 

    if (cgsize > nos + nrs) 
    { System.out.println("*** Call graph too complex: only " + (nos + nrs) + " rules/operations"); } 

    Vector selfcalls = tc.getSelfMaps(); 
    int selfcallsn = selfcalls.size();  
 
    if (selfcallsn > 0) 
    { System.err.println("*** Bad smell: complex call graph with " + selfcallsn + " recursive dependencies"); } 


    System.out.println(); 
    System.out.println(); 


    UseCase uc = mr.toUML(types,entities,res);  
    System.out.println(mr);  

    java.util.Map clones = new java.util.HashMap(); 

    PrintWriter chout; 

    try
    { chout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter("output/measures.txt")));
    } catch (Exception _u) { return; } 

    int topscount = 0; 
    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      // ent.displayMeasures(chout,clones); 
      System.out.println(); 
      topscount = topscount + ent.operationsCount(); 
    } 


    Map mres = new Map(); 

    for (int j = 0; j < entities.size(); j++) 
    { Entity ent = (Entity) entities.get(j); 
      if (ent.isDerived()) { continue; } 

      Map cg = ent.getCallGraph(); 
      if (cg.size() > 0) 
      { // System.out.println("Call graph of entity " + ent.getName() + " is: " + cg); 
        mres = Map.union(mres,cg); 
      }  
    } 

        

    Vector newrules = mr.dataAnalysis();
    if (newrules.size() > 0) 
    { ATLModule mr2 = new ATLModule(mr.name); 
      mr2.setInterpretation(mr.interp); 
      mr2.setElements(mr.elements); 
      mr2.addElements(newrules); 
      uc = mr2.toUML(types,entities,res); 
    } 

    System.out.println("UML-RSDS for ATL code is: " + uc.display());  
    uc.typeCheck(types,entities); 
    // useCases.add(uc);
    addGeneralUseCase(uc);  
  }

  public void loadETL()
  { /* 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ee = (Entity) entities.get(i); 
      if (ee.isSourceEntity())
      { String nme = ee.getName(); 
        ee.setName("IN$" + nme); 
      }  
      else if (ee.isTargetEntity())
      { String nme = ee.getName(); 
        ee.setName("OUT$" + nme); 
      } 
    }  */ 
    auxiliaryMetamodel(); 

    Compiler2 c = new Compiler2(); 
    BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file;

    File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Load ETL file");
    fc.addChoosableFileFilter(new ETLFileFilter()); 
    int returnVal = fc.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { file = fc.getSelectedFile(); }
    else
    { System.err.println("Load aborted");
      return; 
    }

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException _e)
    { System.out.println("File not found: " + file);
      return; 
    }

    String atlstring = ""; 
    int noflines = 0; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException _ex)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else if (s.startsWith("--")) { } 
      else 
      { atlstring = atlstring + s + " "; } 
      noflines++; 
    }
    c.nospacelexicalanalysis(atlstring); 
    
    

    EtlModule mr = c.parseEtl(entities,types); 
    if (mr == null) 
    { System.err.println("Invalid ETL syntax"); 
      return; 
    } 

    Vector cntxs = new Vector(); 
    Vector env = new Vector(); 
    mr.typeCheck(types,entities,cntxs,env); 

    System.out.println(""); 
    System.out.println(); 
    System.err.println("*** " + noflines + " Lines"); 

    int cn = c.checkclones(types,entities);
    System.out.println("*** " + cn + " Clones of size 10 tokens or more");  

    int cc = mr.syntacticComplexity(); 
    System.out.println("*** Complexity of module is " + cc);
    System.out.println();  

    Map ucg = mr.getCallGraph();

    System.out.println("*** Call graph of module is ");
   
    Map mm1 = ucg.removeDuplicates(); 


    Map tc = new Map(); 

    tc.elements = Map.transitiveClosure(mm1.elements);

    System.out.println(mm1); 

    int cgsize = mm1.elements.size(); 
    System.out.println("*** Size of call graph = " + cgsize);  

    int nos = mr.nops(); 
    int nrs = mr.nrules(); 

    if (cgsize > nos + nrs) 
    { System.out.println("*** Call graph too complex: only " + (nos + nrs) + " rules/operations"); } 


    Vector selfcalls = tc.getSelfMaps(); 
    int selfcallsn = selfcalls.size();  
 
    if (selfcallsn > 0) 
    { System.err.println("Bad smell: complex call graph with " + selfcallsn + " recursive dependencies"); } 


    System.out.println(); 
    System.out.println(); 
 
    int enr = mr.enr(); 
    int eno = mr.eno(); 
    int epl = mr.epl(); 
    int uex = mr.uex(); 
    
    System.out.println("*** ENR of module is " + enr); 
    System.out.println("*** ENO of module is " + eno); 
    System.out.println("*** EPL of module is " + epl); 
    System.out.println("*** UEX of module is " + uex); 
    int cyc = mr.cyclomaticComplexity(); 
    System.out.println("*** Rules/operations with CC > 10: " + cyc);
    System.out.println();  

    System.out.println(); 

    UseCase uc = mr.toUseCase(entities,types);  
    System.out.println("UML-RSDS of ETL is: " + mr);  
    
    /* Vector newrules = mr.dataAnalysis();
    if (newrules.size() > 0) 
    { ATLModule mr2 = new ATLModule(mr.name); 
      mr2.setInterpretation(mr.interp); 
      mr2.setElements(mr.elements); 
      mr2.addElements(newrules); 
      uc = mr2.toUML(types,entities); 
    } 
    System.out.println(uc.display());  */ 

    uc.typeCheck(types,entities); 
    // useCases.add(uc);
    addGeneralUseCase(uc);  
  }


  public void loadFlock()
  { 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ee = (Entity) entities.get(i); 
      if (ee.isRoot() && !ee.isInterface())
      { ee.addPrimaryKey("$id"); } 
    } 

    Compiler2 c = new Compiler2(); 
    BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/flock.txt");  /* default */ 

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException _e)
    { System.out.println("File not found: " + file);
      return; 
    }

    String flockstring = ""; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException _ex)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else 
      { flockstring = flockstring + s + " "; } 
    }
    c.nospacelexicalanalysis(flockstring); 
    
        //  c.displaylexs(); 
    FlockModule mr = c.parseFlock(entities,types);
    Vector sourceents = getSourceEntities(); 
    Vector targetents = getTargetEntities(); 

    if (mr == null) 
    { System.err.println("Invalid syntax"); 
      return; 
    } 
 
    UseCase uc = mr.toUseCase(sourceents,targetents);  
    // System.out.println(uc.display());
  
    /* Vector newrules = mr.dataAnalysis();
    if (newrules.size() > 0) 
    { ATLModule mr2 = new ATLModule(mr.name); 
      mr2.setInterpretation(mr.interp); 
      mr2.setElements(mr.elements); 
      mr2.addElements(newrules); 
      uc = mr2.toUML(types,entities); 
    } 
    System.out.println(uc.display()); */   
    uc.typeCheck(types,entities); 
    // useCases.add(uc);
    addGeneralUseCase(uc);  
  }

  public void loadQVT()
  { 
    // for (int i = 0; i < entities.size(); i++) 
    // { Entity ee = (Entity) entities.get(i); 
    //   if (ee.isRoot() && !ee.isInterface())
    //   { ee.addPrimaryKey("$id"); } 
    // } 

    Compiler2 c = new Compiler2(); 
    BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/mm.qvt");  /* default */ 

    BufferedWriter brout = null; 
    PrintWriter pwout = null; 

    File outfile = new File("output/qvtrmeasures.txt"); 

    try
    { br = new BufferedReader(new FileReader(file));
      brout = new BufferedWriter(new FileWriter(outfile)); 
      pwout = new PrintWriter(brout); 
    }
    catch (Exception _e)
    { System.out.println("!!!! File not found: " + file);
      return; 
    }

    String flockstring = ""; 
    int noflines = 0; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException _ex)
      { System.out.println("!!! Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else if (s.startsWith("--")) { } 
      else 
      { flockstring = flockstring + s + " "; } 
      noflines++; 
    }
    c.nospacelexicalanalysis(flockstring); 
    
    // c.displaylexs();
    RelationalTransformation tt = c.parse_QVTR(0,c.lexicals.size()-1,entities,types);
    if (tt == null) 
    { System.err.println("!!!! Invalid QVT-R syntax"); 
      return; 
    } 
 
    System.out.println("**** Parsed QVT-R: " + tt); 
    tt.typeCheck(types,entities,new Vector(),new Vector()); 
    // System.out.println(entities); 

    // FlockModule mr = c.parseFlock(entities,types);
    // Vector sourceents = getSourceEntities(); 
    // Vector targetents = getTargetEntities(); 

    int qvtflaws = 0; 
    if (noflines > 500) 
    { pwout.println("*** ETS flaw -- transformation > 500 LOC"); 
      qvtflaws++; 
    } 

    pwout.println(); 
    pwout.println("*** " + noflines + " Lines"); 
    int cn = c.checkclones(types,entities);
    qvtflaws += cn; 
    pwout.println("*** " + cn + " Clones of size 10 tokens or more (DC flaws)");  

    int comp = tt.complexity(pwout); 
    pwout.println("*** Complexity of module = " + comp); 

    int qvtepl = tt.epl(pwout); 
    qvtflaws += qvtepl; 
    pwout.println("*** rules with EPL > 10 = " + qvtepl);

    int qvtenr = tt.enr(); 
    if (qvtenr > 10) 
    { qvtflaws++; }  
    pwout.println("*** ENR of module = " + qvtenr);

    int qvteno = tt.eno(); 
    if (qvteno > 10) 
    { qvtflaws++; }   
    pwout.println("*** ENO of module = " + qvteno); 

    int qvtuex = tt.uex(); 
    if (qvtuex > 10)
    { qvtflaws++; } 
    pwout.println("*** UEX of module = " + qvtuex); 
    Map ucg = tt.getCallGraph();

    pwout.println("*** Call graph of module " + tt.getName() + " is: "); 

    Map mm1 = ucg.removeDuplicates(); 

    pwout.println(mm1); 

    Map tc = new Map(); 

    tc.elements = Map.transitiveClosure(mm1.elements);

    int cgsize = mm1.elements.size(); 
    pwout.println("*** Size of call graph = " + cgsize);  

    int nos = tt.nops(); 
    int nrs = tt.nrules(); 

    if (cgsize > nos + nrs) 
    { pwout.println("*** Call graph too complex: size is " + 
                    cgsize + " > number " + (nos + nrs) + " of rules/operations"); 
      qvtflaws++; // CBR_1 flaw
    } 


    Vector selfcalls = tc.getSelfMaps(); 
    int selfcallsn = selfcalls.size();  
 
    if (selfcallsn > 0) 
    { pwout.println("*** Bad smell: complex call graph with " + 
                    selfcallsn + " cyclic dependencies"); 
      qvtflaws += selfcallsn; // CBR_2 flaws    
    } 


    pwout.println(); 
    pwout.println(); 
 
    int cyc = tt.cyclomaticComplexity(); 
    qvtflaws += cyc; 
    pwout.println("*** Rules/operations with CC > 10: " + cyc);
    pwout.println();  

    pwout.println(); 
    pwout.println(); 
    pwout.println("*** Total number of flaws: " + qvtflaws);
 
    // also count ERS, EHS


    Vector assocs = new Vector(); 

    tt.checkOrdering(); 

    RelationalTransformation newtrans = tt.expandOverrides(entities); 

    System.out.println("**** Expanded QVT-R: " + newtrans); 

    newtrans.addTraceEntities(entities,assocs);  
    associations.addAll(assocs); 

    Vector ucs = newtrans.toUseCase(entities,types);  
    for (int i = 0; i < ucs.size(); i++) 
    { UseCase uc = (UseCase) ucs.get(i); 
      uc.setBx(true); 
      System.out.println("**** UML-RSDS of QVT-R is: " + uc.display());
      
      uc.typeCheck(types,entities); 
      // useCases.add(uc);
      addGeneralUseCase(uc);
    }

    pwout.close(); 
    // System.out.println("New associations: " + assocs);    
  }


  public void loadGenericUseCase()
  { BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file;

    File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Load generic use case");
    int returnVal = fc.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { file = fc.getSelectedFile(); }
    else
    { System.err.println("Load aborted");
      return; 
    }

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException e)
    { System.out.println("File not found: " + file);
      return; 
    }

    Vector preconstraints = new Vector(); 
    Vector preassertions = new Vector(); 
    Vector genericucs = new Vector(); 
    Vector preucinvs = new Vector(); 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      System.out.println("Reading file"); 

      if (s == null) 
      { eof = true; 
        System.out.println("End of file"); 
        break; 
      }
      else if (s.equals("Constraint:"))
      { PreConstraint c = parseConstraint(br);
        if (c != null)
        { preconstraints.add(c); }
      } 
      else if (s.equals("Assumption:"))
      { PreConstraint ac = parseConstraint(br);
        if (ac != null)
        { preassertions.add(ac); }
      } 
      else if (s.equals("UseCaseInvariant:"))
      { PreConstraint ac = parseConstraint(br);
        if (ac != null)
        { preucinvs.add(ac); }
      } 
      else if (s.equals("GeneralUseCase:"))
      { UseCase uc = parseGeneralUseCase(br); 
        // useCases.add(uc); 
        genericucs.add(uc); 
      } 
      else 
      { System.out.println("Unrecognised input: " + s); } 
    } 
    try { br.close(); } catch(IOException e) { }

    // add each pre and post condition to the corresponding uc, but do not 
    // type check until instantiated: 

    for (int i = 0; i < preassertions.size(); i++) 
    { PreConstraint pc = (PreConstraint) preassertions.get(i); 
      addGenericAssertion(pc,genericucs); 
    } 

    for (int i = 0; i < preconstraints.size(); i++) 
    { PreConstraint pc = (PreConstraint) preconstraints.get(i); 
      addGenericInvariant(pc,genericucs); 
    } 

    for (int i = 0; i < genericucs.size(); i++) 
    { UseCase gen = (UseCase) genericucs.get(i); 
      Vector pars = gen.getParameters(); 
      Vector avals = new Vector(); 

      if (pars.size() > 0)
      { 
        System.out.println("Define actual values for parameters " + pars); 
        for (int j = 0; j < pars.size(); j++)
        { String apar = 
            JOptionPane.showInputDialog("Actual value of " + pars.get(j) + ":");
          Compiler2 comp = new Compiler2(); 
          comp.nospacelexicalanalysis(apar); 
          Expression parexp = comp.parse();
          while (parexp == null) 
          { System.err.println("ERROR: Invalid parameter expression: " + parexp); 
            apar = 
              JOptionPane.showInputDialog("Actual value of " + pars.get(j) + ": ");
            comp = new Compiler2(); 
            comp.nospacelexicalanalysis(apar); 
            parexp = comp.parse();
          } 
          avals.add(parexp);
        } 
      } 
      UseCase newuc = gen.instantiate(avals,types,entities,associations); 
      useCases.add(newuc); 
    } 
  } // the generic use case is in its own file

  public void typeCheckOps()
  { System.out.println("Rechecking operations"); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      ent.typeCheckOps(types,entities); 
    } 
  } 

  public void loadFromCSV(String f)
  { loadFromFile("excel.mm"); 
    Entity excel = (Entity) ModelElement.lookupByName("ExcelLib", entities); 
    if (excel == null) 
    { System.err.println("Library ExcelLib in output/excel.mm is needed to use Excel functions!"); 
      return; 
    } 

    BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/mm.csv");  /* default */ 

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException e)
    { System.out.println("File not found: " + file);
      return; 
    }

    Vector rows = new Vector(); 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else 
      { Vector row = parseCSVRow(s); 
        if (row != null)
        { rows.add(row);
          System.out.println("Parsed row: " + row); 
        }
      } 
    } 

    Vector ents = analyseCSVRows(rows, excel); 
    analyseCSVEntities(ents); 

    try { br.close(); } catch(IOException e) { }
  } 

  private boolean isClassNameRow(Vector row) 
  { boolean result = false; 
    for (int j = 0; j < row.size(); j++) 
    { String str = (String) row.get(j); 
      if (str != null && str.endsWith(":")) 
      { return true; } 
    } 
    return result; 
  } 

  private String getClassNameRow(Vector row) 
  { String result = null; 
    for (int j = 0; j < row.size(); j++) 
    { String str = (String) row.get(j); 
      if (str != null && str.endsWith(":")) 
      { return str.substring(0,str.length()-1); } 
    } 
    return result; 
  } 

  private boolean isVariableDefinitionRow(Vector row) 
  { boolean result = false; 
    for (int j = 0; j < row.size(); j++) 
    { String str = (String) row.get(j); 
      if (str != null) 
      { String[] strs = str.split(" "); 
        if (strs.length > 1 && "=".equals(strs[1])) // && strs.get(0) is an identifier 
        { return true; } 
      } 
    } 
    return result; 
  } 

  private String getVariableNameRow(Vector row) 
  { String result = null; 
    for (int j = 0; j < row.size(); j++) 
    { String str = (String) row.get(j); 
      if (str != null)
      { String[] strs = str.split(" "); 
        if (strs.length > 1 && "=".equals(strs[1])) // && str.get(0) is an identifier 
        { return strs[0]; } 
      } 
    } 
    return result; 
  } 

  private String getVariableDefinitionRow(Vector row) 
  { String result = null; 
    for (int j = 0; j < row.size(); j++) 
    { String str = (String) row.get(j); 
      if (str != null)
      { String[] strs = str.split(" "); 
        if (strs.length > 1 && "=".equals(strs[1])) // && str.get(0) is an identifier 
        { int ind = str.indexOf("="); 
          String def = str.substring(ind + 1, str.length()); 
          return def.trim(); 
        } 
      } 
    } 
    return result; 
  } 

  private Vector analyseCSVRows(Vector rows, Entity excel)
  { // Each row can be a class table header: list of attribute names
    // or list of attribute example values or expressions. 

    Vector preents = new Vector(); 
    Entity current = null; 
    Vector preatts = new Vector(); 

    for (int i = 0; i < rows.size(); i++) 
    { Vector row = (Vector) rows.get(i); 
     
      if (row != null && row.size() > 0) 
      { if (current == null && isVariableDefinitionRow(row))
        { String vname = getVariableNameRow(row); 
          String vdef = getVariableDefinitionRow(row); 
          Compiler2 c = new Compiler2(); 
          c.nospacelexicalanalysis(vdef); 
          Expression e = c.parse(); 
          System.out.println("VARIABLE DEFINITION: " + vname + " " + e); 
          Vector vdefv = new Vector(); 
          vdefv.add(vname); vdefv.add(e); 
          preents.add(vdefv); 
        } 
        else if (current == null && isClassNameRow(row)) // Line is a new entity header
        { String ename = getClassNameRow(row); 
          i++;  
          row = (Vector) rows.get(i);
          for (int j = 0; j < row.size(); j++) 
          { String att = (String) row.get(j); 
            if (att != null && att.length() > 0) 
            { if (current == null) // first row, first column of entity
              { current = new Entity(ename); 
                RectData rd = new RectData(20*(j+1), 200 + 30*(i+1), Color.BLACK, SENSOR, preents.size()); 
                rd.setModelElement(current); 
                rd.setLabel(ename); 
                visuals.add(rd); 
                entities.add(current);
                current.setSuperclass(excel); 
                excel.addSubclass(current); 
                preatts.add(att);
                preents.add(current);   
              } 
              else // first row, other columns  
              { preatts.add(att); } 
            } 
          } 
        } 
        else // 2nd row
        { int attcount = 0; 
          for (int k = 0; k < row.size(); k++) 
          { String attval = (String) row.get(k); 
            if (attval != null && attval.length() > 0) 
            { Compiler2 co = new Compiler2(); 
              co.nospacelexicalanalysis(attval); 
              Expression epr = co.parse(); 
              if (epr == null) 
              { System.err.println("Invalid expression: " + attval); } 
              else
              { Vector contexts = new Vector(); 
                contexts.add(excel); 
                epr.typeCheck(types, entities, contexts, new Vector());
                Vector qvars = new Vector(); 
                Vector antes = new Vector(); 
                Expression expr = epr.excel2Ocl(current, entities, qvars, antes);   
                Type t = expr.getType(); 
                // System.out.println("EXCEL EXPRESSION: " + expr); 

                Attribute newatt = new Attribute((String) preatts.get(attcount), t, ModelElement.INTERNAL);
                if (epr.umlkind == Expression.VALUE) 
                { newatt.setInitialValue(attval); }  
                newatt.setInitialExpression(epr);
                newatt.setElementType(expr.getElementType()); // should be ok  
                current.addAttribute(newatt); 
                newatt.setEntity(current); 
                attcount++; 
              } 
            } 
          } 
          current = null; 
          preatts.clear(); 
        } 
      } 
    } 
    return preents; 
  }  
  // For each entity, if it is a target entity - it has some non-value attval - 
  // create a constraint. 

  private void analyseCSVEntities(Vector ents)
  { UseCase uc = new UseCase("sheet"); 
    Constraint createApp = Constraint.createAppCons(entities); 
    uc.addPostcondition(createApp); 
    
    for (int i = 0; i < ents.size(); i++) 
    { if (ents.get(i) instanceof Entity) 
      { Entity e = (Entity) ents.get(i); 
        if (e.getName().equals("ExcelLib") || e.hasStereotype("auxilliary")) { } 
        else 
        { Vector atts = e.getAttributes(); 
          boolean istarget = false; 
          Vector setatts = new Vector(); 
          Vector setexps = new Vector(); 

          for (int j = 0; j < atts.size(); j++) 
          { Attribute att = (Attribute) atts.get(j); 
            Expression einit = att.getInitialExpression(); 
            if (einit == null) { } // ignore
            else if (einit.umlkind == Expression.VALUE) 
            { setatts.add(att); 
              setexps.add(einit); 
            } 
            else 
            { istarget = true; 
              setatts.add(att); 
              setexps.add(einit);
              att.setInitialExpression(null);  // to avoid invalid computations.  
            } 
          }
          if (istarget) 
          { Constraint con = Constraint.fromExcel(entities, e, setatts, setexps); 
            uc.addPostcondition(con); 
          } 
        }
      }  
      else if (ents.get(i) instanceof Vector) // variable or function definition
      { Vector deff = (Vector) ents.get(i); 
        String fname = (String) deff.get(0); 
        Expression fdef = (Expression) deff.get(1); 
        Vector env = new Vector(); 
        Vector context = new Vector(); 
        Entity excellib = (Entity) ModelElement.lookupByName("ExcelLib", entities);
        if (excellib != null) 
        { context.add(excellib); } 
        fdef.typeCheck(types,entities,context,env); 
        Attribute att = new Attribute(fname, fdef.getType(), ModelElement.INTERNAL); 
        att.setElementType(fdef.getElementType()); 

        uc.addAttribute(att); // must be static
        BasicExpression fexp = new BasicExpression("Sheet." + fname); 
        fexp.setType(fdef.getType()); 
        fexp.setElementType(fdef.getElementType()); 

        Constraint conf = new Constraint(new BasicExpression(true), 
                                 new BinaryExpression("=", fexp, fdef)); 
        uc.addPostcondition(conf);  
      } // parameters are needed.  
    } 
    addGeneralUseCase(uc); 
    uc.typeCheck(types, entities); 
  }  


  /*                 Vector qvars = new Vector(); 
                Vector antes = new Vector(); 
                Expression oclexp = epr.excel2Ocl(current, entities, qvars, antes); 
                if (qvars.size() == 0) 
                { newatt.setInitialExpression(oclexp); }  
  */ 

  public void loadFromFile(String f)
  { BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/" + f);  /* default */ 

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException e)
    { System.out.println("File not found: " + file);
      return; 
    }

    Vector preentities = new Vector(); 
    Vector preassociations = new Vector(); 
    Vector pregeneralisations = new Vector();
    Vector preconstraints = new Vector(); 
    Vector preassertions = new Vector(); 
    Vector preops = new Vector(); 
    Vector pucs = new Vector(); 
    Vector preactivities = new Vector(); 
    Vector preucinvs = new Vector(); 
    Vector preoperators = new Vector(); 

    int linecount = 0; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else if (s.startsWith("--")) { }  // comment line
      else if (s.startsWith("Import:"))
      { String importfile = ""; 
        try { 
          importfile = br.readLine(); 
          String fle = importfile.trim(); 
          if (imported.contains(fle)) { } 
          else 
          { loadFromFile(fle); 
            imported.add(fle); 
          } 
        } catch (IOException _excep) 
          { System.out.println("Cannot load imported file " + importfile); 
            continue; 
          }  
      }     
      else if (s.startsWith("EntityMapping:"))
      { try { String emap = br.readLine(); 
          String trimemap = emap.trim(); 
          int mapsymb = trimemap.indexOf("|"); 
          if (mapsymb > 0) 
          { String sents = trimemap.substring(0,mapsymb); 
            String tents = trimemap.substring(mapsymb + 3, trimemap.length()); 
            Entity esrc = (Entity) ModelElement.lookupByName(sents.trim(),entities); 
            Entity etrg = (Entity) ModelElement.lookupByName(tents.trim(),entities); 
 
            if (esrc != null && etrg != null) 
            { EntityMatching em = new EntityMatching(esrc,etrg);
              System.out.println("Using match: " + sents + " to " + tents);
              entitymaps.add(em);
            }   
          } 
        } catch (IOException _ex) { } 
      }         
      else if (s.startsWith("Entity:"))
      { PreEntity pe = parseEntity(br); 
        if (pe != null)
        { preentities.add(pe); }
      } 
      else if (s.equals("Operator:"))
      { PreOperator pop = parseOperator(br); 
        if (pop != null)
        { preoperators.add(pop); }
      } 
      else if (s.equals("Association:"))
      { PreAssociation pa = parseAssociation(br); 
        if (pa != null) 
        { preassociations.add(pa); } 
      }
      else if (s.equals("Constraint:"))
      { PreConstraint c = parseConstraint(br);
        if (c != null)
        { preconstraints.add(c); }
      } 
      else if (s.equals("Assumption:"))
      { PreConstraint ac = parseConstraint(br);
        if (ac != null)
        { preassertions.add(ac); }
      } 
      else if (s.equals("UseCaseInvariant:"))
      { PreConstraint ac = parseConstraint(br);
        if (ac != null)
        { preucinvs.add(ac); }
      } 
      else if (s.equals("Type:"))
      { parseType(br); } 
      else if (s.equals("Generalisation:"))
      { PreGeneralisation pg = parseGeneralisation(br); 
        pregeneralisations.add(pg); 
      } 
      else if (s.equals("Operation:"))
      { PreOp po = parseOperation(br);
        preops.add(po); 
      } 
      else if (s.equals("UseCase:"))
      { PreUseCase puc = parseUseCase(br); 
        pucs.add(puc); 
      } 
      else if (s.equals("GeneralUseCase:"))
      { UseCase uc = parseGeneralUseCase(br); 
        if (uc != null) { useCases.add(uc); } 
        // OvalData od = new OvalData(10,80*useCases.size(),getForeground(),useCases.size()); 
        // od.setName(uc.getName()); 
        // od.setModelElement(uc); 
        // visuals.add(od); 
      } 
      else if (s.equals("Activity:"))
      { PreBehaviour bb = parseBehaviour(br); 
        if (bb != null)
        { preactivities.add(bb); } 
      } 
      linecount++; 
    } 
    try { br.close(); } catch(IOException e) { }

    System.out.println("**** Line count: " + linecount); 

    for (int i = 0; i < preassociations.size(); i++)
    { PreAssociation p =
        (PreAssociation) preassociations.get(i);
      reconstructAssociation(p.e1name,p.e2name,p.xs,p.ys,
                             p.xe,p.ye,p.card1,p.card2,
                             p.role2,p.role1,p.stereotypes, p.wpoints);
    }

    for (int j = 0; j < preentities.size(); j++)
    { PreEntity pe = (PreEntity) preentities.get(j);
      int n = pe.attnames.size();
      for (int k = 0; k < n; k++)
      { String attn = (String) pe.attnames.get(k);
        String attt = (String) pe.tnames.get(k);
        String afroz = (String) pe.attfroz.get(k); 
        String auniq = (String) pe.attuniq.get(k); 
        String astatic = (String) pe.attstatic.get(k); 


        Type t = Type.getTypeFor(attt,types,entities);
        if (t == null)  // assume its a standard type
        { JOptionPane.showMessageDialog(null, "Warning: no type for " + attt); 
          t = new Type(attt,null); 
          types.add(t); 
        } 

        int attm = pe.attmodes[k];
        boolean froz = Expression.toBoolean(afroz); 
        boolean uniq = Expression.toBoolean(auniq); 
        boolean stat = Expression.toBoolean(astatic); 

        Attribute att = new Attribute(attn,t,attm);
        att.setInstanceScope(!stat); 
        att.setFrozen(froz); 
        att.setUnique(uniq); 
        att.setElementType(t.getElementType()); 

        // JOptionPane.showMessageDialog(null,"Reconstructed attribute: " + att + " " + t + " " + att.getElementType()); 
        pe.e.addAttribute(att);
        att.setEntity(pe.e); 
      }
    }

    for (int h = 0; h < preoperators.size(); h++)
    { PreOperator preop = (PreOperator) preoperators.get(h); 
      Type opt =
          (Type) ModelElement.lookupByName(preop.type,types);
      if (opt == null)  
      { Entity opte = (Entity) ModelElement.lookupByName(preop.type, entities); 
        if (opte != null) 
        { opt = new Type(opte); }
        else 
        { opt = new Type(preop.type, null); } 
      } 
      Expression.addOperator(preop.name, opt); 
      if (preop.javacode != null && preop.javacode.length() > 0)
      { Expression.addOperatorJava(preop.name, preop.javacode); } 
      if (preop.csharpcode != null && preop.csharpcode.length() > 0)
      { Expression.addOperatorCSharp(preop.name, preop.csharpcode); } 
    } // C++ also 

    for (int q = 0; q < pregeneralisations.size(); q++)
    { PreGeneralisation pg = (PreGeneralisation) pregeneralisations.get(q); 
      reconstructGeneralisation(pg); 
    } 

    for (int r = 0; r < preops.size(); r++)
    { PreOp po = (PreOp) preops.get(r); 
      BehaviouralFeature b = reconstructOperation(po); 
    }

    for (int p = 0; p < preconstraints.size(); p++)
    { PreConstraint pc = (PreConstraint) preconstraints.get(p);
      Constraint c = addInvariant(pc); 
      // reconstructConstraint(pc); 
    }

    for (int p = 0; p < preassertions.size(); p++)
    { PreConstraint pc = (PreConstraint) preassertions.get(p);
      Constraint ac = addAssertion(pc); 
      // reconstructConstraint(pc); 
    }

    for (int p = 0; p < preucinvs.size(); p++)
    { PreConstraint pc = (PreConstraint) preucinvs.get(p);
      Constraint ac = addUCInvariant(pc); 
      // reconstructConstraint(pc); 
    }

    for (int w = 0; w < pucs.size(); w++)
    { PreUseCase pu = (PreUseCase) pucs.get(w); 
      reconstructUseCase(pu.nme,pu.ent,pu.role); 
    } 

    for (int z = 0; z < preactivities.size(); z++)
    { PreBehaviour pb = (PreBehaviour) preactivities.get(z); 
      reconstructBehaviour(pb); 
    } 

    typeCheckOps(); 

    repaint(); 
  }


  public void loadFromFile()
  { BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file;
      // = new File("out.dat");  /* default */ 

    File startingpoint = new File("output");
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(startingpoint);
    fc.setDialogTitle("Load system data");
    fc.addChoosableFileFilter(new TextFileFilter()); 

    int returnVal = fc.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION)
    { file = fc.getSelectedFile(); }
    else
    { System.err.println("Load aborted");
      return; 
    }

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException e)
    { System.out.println("File not found: " + file);
      return; 
    }

    Vector preentities = new Vector(); 
    Vector preassociations = new Vector(); 
    Vector pregeneralisations = new Vector();
    Vector preconstraints = new Vector(); 
    Vector preassertions = new Vector(); 
    Vector preops = new Vector(); 
    Vector pucs = new Vector(); 
    Vector preucinvs = new Vector(); 
    Vector preactivities = new Vector(); 
    Vector preoperators = new Vector(); 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else if (s.startsWith("--")) { }  // comment line
      else if (s.startsWith("Import:"))
      { String importfile = ""; 
        try { 
          importfile = br.readLine(); 
          String fle = importfile.trim(); 
          loadFromFile(fle);
        } catch (IOException _excep) 
          { System.out.println("Cannot load imported file " + importfile); 
            continue; 
          }  
      } 
      else if (s.startsWith("EntityMapping:"))
      { try { String emap = br.readLine(); 
          String trimemap = emap.trim(); 
          int mapsymb = trimemap.indexOf("|"); 
          if (mapsymb > 0) 
          { String sents = trimemap.substring(0,mapsymb); 
            String tents = trimemap.substring(mapsymb + 3, trimemap.length()); 
            System.out.println("Map: " + sents + " to " + tents); 
          } 
        } catch (IOException _ex) { } 
      }         
      else if (s.equals("Entity:"))
      { PreEntity pe = parseEntity(br); 
        if (pe != null)
        { preentities.add(pe); }
      } 
      else if (s.equals("Operator:"))
      { PreOperator pop = parseOperator(br); 
        if (pop != null)
        { preoperators.add(pop); }
      } 
      else if (s.equals("Association:"))
      { PreAssociation pa = parseAssociation(br); 
        if (pa != null) 
        { preassociations.add(pa); } 
      }
      else if (s.equals("Constraint:"))
      { PreConstraint c = parseConstraint(br);
        if (c != null)
        { preconstraints.add(c); }
      } 
      else if (s.equals("Assumption:"))
      { PreConstraint ac = parseConstraint(br);
        if (ac != null)
        { preassertions.add(ac); }
      } 
      else if (s.equals("UseCaseInvariant:"))
      { PreConstraint ac = parseConstraint(br);
        if (ac != null)
        { preucinvs.add(ac); }
      } 
      else if (s.equals("Type:"))
      { parseType(br); } 
      else if (s.equals("Generalisation:"))
      { PreGeneralisation pg = parseGeneralisation(br); 
        pregeneralisations.add(pg); 
      } 
      else if (s.equals("Operation:"))
      { PreOp po = parseOperation(br);
        preops.add(po); 
      } 
      else if (s.equals("UseCase:"))
      { PreUseCase puc = parseUseCase(br); 
        pucs.add(puc); 
      } 
      else if (s.equals("GeneralUseCase:"))
      { UseCase uc = parseGeneralUseCase(br); 
        if (uc != null) { useCases.add(uc); }  
        // OvalData od = new OvalData(10,80*useCases.size(),getForeground(),useCases.size()); 
        // od.setName(uc.getName()); 
        // od.setModelElement(uc); 
        // visuals.add(od); 
      } 
      else if (s.equals("Activity:"))
      { PreBehaviour bb = parseBehaviour(br); 
        if (bb != null)
        { preactivities.add(bb); } 
      } 
    } 
    try { br.close(); } catch(IOException e) { }

    // System.out.println("Preentities: " + preentities); 

    for (int i = 0; i < preassociations.size(); i++)
    { PreAssociation p =
        (PreAssociation) preassociations.get(i);
      reconstructAssociation(p.e1name,p.e2name,p.xs,p.ys,
                             p.xe,p.ye,p.card1,p.card2,
                             p.role2,p.role1,p.stereotypes, p.wpoints);
    }

    for (int j = 0; j < preentities.size(); j++)
    { PreEntity pe = (PreEntity) preentities.get(j);
      int n = pe.attnames.size();
      for (int k = 0; k < n; k++)
      { String attn = (String) pe.attnames.get(k);
        String attt = (String) pe.tnames.get(k);
        String afroz = (String) pe.attfroz.get(k); 
        String auniq = (String) pe.attuniq.get(k); 
        String astatic = (String) pe.attstatic.get(k); 

        Type t = Type.getTypeFor(attt,types,entities);
        if (t == null)  // assume its a standard type
        { JOptionPane.showMessageDialog(null, "Possible error: no type for " + attt); 
          t = new Type(attt,null); 
          types.add(t); 
        } 
        int attm = pe.attmodes[k];
        boolean froz = Expression.toBoolean(afroz); 
        boolean uniq = Expression.toBoolean(auniq); 
        boolean stat = Expression.toBoolean(astatic); 

        Attribute att = new Attribute(attn,t,attm);
        att.setInstanceScope(!stat); 
        att.setFrozen(froz); 
        att.setUnique(uniq); 
        att.setElementType(t.getElementType()); 

        // JOptionPane.showMessageDialog(null, "Reconstructed attribute: " + att + " type " + t + " element type " + att.getElementType()); 
        pe.e.addAttribute(att);
        att.setEntity(pe.e); 
      }
    }

    for (int h = 0; h < preoperators.size(); h++)
    { PreOperator preop = (PreOperator) preoperators.get(h); 
      Type opt = Type.getTypeFor(preop.type,types,entities);
      if (opt == null)  
      { Entity opte = (Entity) ModelElement.lookupByName(preop.type, entities); 
        if (opte != null) 
        { opt = new Type(opte); }
        else 
        { opt = new Type(preop.type, null); } 
      } 
      Expression.addOperator(preop.name, opt); 
      if (preop.javacode != null && preop.javacode.length() > 0)
      { Expression.addOperatorJava(preop.name, preop.javacode); } 
      if (preop.csharpcode != null && preop.csharpcode.length() > 0)
      { Expression.addOperatorCSharp(preop.name, preop.csharpcode); } 
    } // and C++

    for (int q = 0; q < pregeneralisations.size(); q++)
    { PreGeneralisation pg = (PreGeneralisation) pregeneralisations.get(q); 
      reconstructGeneralisation(pg); 
    } 

    for (int r = 0; r < preops.size(); r++)
    { PreOp po = (PreOp) preops.get(r); 
      BehaviouralFeature b = reconstructOperation(po); 
    }

    for (int p = 0; p < preconstraints.size(); p++)
    { PreConstraint pc = (PreConstraint) preconstraints.get(p);
      Constraint c = addInvariant(pc); 
      // reconstructConstraint(pc); 
    }

    for (int p = 0; p < preassertions.size(); p++)
    { PreConstraint pc = (PreConstraint) preassertions.get(p);
      Constraint ac = addAssertion(pc); 
      // reconstructConstraint(pc); 
    }

    for (int p = 0; p < preucinvs.size(); p++)
    { PreConstraint pc = (PreConstraint) preucinvs.get(p);
      Constraint ac = addUCInvariant(pc); 
      // reconstructConstraint(pc); 
    }

    for (int w = 0; w < pucs.size(); w++)
    { PreUseCase pu = (PreUseCase) pucs.get(w); 
      reconstructUseCase(pu.nme,pu.ent,pu.role); 
    } 

    for (int z = 0; z < preactivities.size(); z++)
    { PreBehaviour pb = (PreBehaviour) preactivities.get(z); 
      reconstructBehaviour(pb); 
    } 

    typeCheckOps(); 

    repaint(); 
  }

  public Vector loadThesaurus()
  { BufferedReader br = null;
    // BufferedWriter brout = null; 
    // PrintWriter pwout = null; 

    Vector concepts = new Vector(); 

    String s;
    boolean eof = false;
    File infile = new File("output/thesaurus.txt");  /* default */ 
    // File outfile = new File("output/model.txt"); 

    try
    { br = new BufferedReader(new FileReader(infile));
      // brout = new BufferedWriter(new FileWriter(outfile)); 
      // pwout = new PrintWriter(brout); 
    }
    catch (Exception e)
    { System.out.println("Errors with file: " + infile);
      return concepts; 
    }
    String xmlstring = ""; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return concepts; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else 
      { xmlstring = xmlstring + s + " "; } 
    }

    Compiler2 comp = new Compiler2();  
    comp.nospacelexicalanalysisxml(xmlstring); 
    XMLNode xml = comp.parseXML(); 
    // System.out.println(xml); 

    if (xml == null) 
    { System.err.println("!!!! Wrong format for XML file. Must start with <?xml header"); 
      return concepts; 
    } 


    Vector enodes = xml.getSubnodes(); // all instances
    for (int i = 0; i < enodes.size(); i++) 
    { XMLNode enode = (XMLNode) enodes.get(i); 
      String cname = enode.getTag(); 
      if ("CONCEPT".equals(cname))
      { ThesaurusConcept c = null; 
        Vector subnodes = enode.getSubnodes(); 
        for (int j = 0; j < subnodes.size(); j++) 
        { XMLNode sb = (XMLNode) subnodes.get(j); 
          String stag = sb.getTag(); 
          if ("DESCRIPTOR".equals(stag))
          { String cdef = sb.getContent(); 
            c = new ThesaurusConcept(cdef.toLowerCase());
            System.out.println("New concept: " + cdef); 
          } 
          else if ("PT".equals(stag) && c != null)
          { String ndef = sb.getContent(); 
            ThesaurusTerm tt = new ThesaurusTerm(ndef.toLowerCase()); 
            c.addPreferredTerm(tt); 
            tt.addConcept(c); 
          } 
          else if ("NT".equals(stag) && c != null)
          { String ndef = sb.getContent(); 
            ThesaurusTerm tt = new ThesaurusTerm(ndef.toLowerCase()); 
            c.addTerm(tt); 
            tt.addConcept(c); 
          } 
        } 
        if (c != null) 
        { concepts.add(c); }  
      } 
    } 

    for (int i = 0; i < concepts.size(); i++) 
    { ThesaurusConcept tc = (ThesaurusConcept) concepts.get(i); 
      tc.findLinkedConcepts(concepts); 
    } 

    return concepts; 
  }       

  public void convertXsiToData()
  { BufferedReader br = null;
    BufferedWriter brout = null; 
    PrintWriter pwout = null; 

    Vector res = new Vector();
    String s;
    boolean eof = false;
    File infile = new File("output/xsi.txt");  /* default */ 
    File outfile = new File("output/model.txt"); 

    try
    { br = new BufferedReader(new FileReader(infile));
      brout = new BufferedWriter(new FileWriter(outfile)); 
      pwout = new PrintWriter(brout); 
    }
    catch (Exception e)
    { System.out.println("Errors with files: " + infile + " " + outfile);
      return; 
    }
    String xmlstring = ""; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else 
      { xmlstring = xmlstring + s + " "; } 
    }

    Compiler2 comp = new Compiler2();  
    comp.nospacelexicalanalysisxml(xmlstring); 
    XMLNode xml = comp.parseXML(); 
    // System.out.println(xml); 

    java.util.Map instancemap = new java.util.HashMap(); // String --> Vector 
    java.util.Map idmap = new java.util.HashMap();       // String --> String
    Vector entcodes = new Vector(); 

    for (int i = 0; i < entities.size(); i++) 
    { Entity et = (Entity) entities.get(i);
      String ename = et.getName().toLowerCase() + "s";  
      instancemap.put(ename,new Vector()); 
      entcodes.add(ename); 
    } 

    Vector enodes = xml.getSubnodes(); // all instances
    for (int i = 0; i < enodes.size(); i++) 
    { XMLNode enode = (XMLNode) enodes.get(i); 
      String cname = enode.getTag(); 
      Vector einstances = (Vector) instancemap.get(cname); 
      if (einstances == null) 
      { einstances = (Vector) instancemap.get(cname + "s"); } // For multiplicity ONE globally
      if (einstances != null) 
      { einstances.add(enode); }  
    } 
      
    for (int j = 0; j < entities.size(); j++) 
    { Entity et = (Entity) entities.get(j); 
      String ename = (String) entcodes.get(j); 
      Vector elems = (Vector) instancemap.get(ename); 
      for (int k = 0; k < elems.size(); k++) 
      { XMLNode enode = (XMLNode) elems.get(k);
        String tname = enode.getAttributeValue("xsi:type"); 
        if (tname == null) 
        { tname = et.getName(); } 
        else 
        { int colonind = tname.indexOf(":"); 
          if (colonind >= 0)
          { tname = tname.substring(colonind + 1,tname.length()); }
        }   
        pwout.println(ename + "" + k + " : " + tname); 

        String idval = enode.getAttributeValue("xmi:id");
        if (idval != null) 
        { idmap.put(idval,ename + "" + k); }  
      } 
    } 

    for (int j = 0; j < entities.size(); j++) 
    { Entity et = (Entity) entities.get(j); 
      String ename = (String) entcodes.get(j); 
      Vector elems = (Vector) instancemap.get(ename); 
      for (int k = 0; k < elems.size(); k++) 
      { XMLNode enode = (XMLNode) elems.get(k);
        Vector atts = enode.getAttributes();
        for (int p = 0; p < atts.size(); p++) 
        { XMLAttribute patt = (XMLAttribute) atts.get(p); 
          if (patt.getName().equals("xsi:type") || patt.getName().equals("xmi:id")) { } 
          else 
          { patt.getDataDeclaration(pwout,ename + k, et, idmap); } 
        } 
      }  
    } 
    try { pwout.close(); } catch (Exception ee) { } 
  }

  public void convertXsiToData(String xmlstring)
  { BufferedWriter brout = null; 
    PrintWriter pwout = null; 
    File outfile = new File("output/model.txt"); 

    try
    { brout = new BufferedWriter(new FileWriter(outfile)); 
      pwout = new PrintWriter(brout); 
    }
    catch (Exception e)
    { System.out.println("Errors with file: " + outfile);
      return; 
    }

    Vector res = new Vector();

    Compiler2 comp = new Compiler2();  
    comp.nospacelexicalanalysisxml(xmlstring); 
    XMLNode xml = comp.parseXML(); 
    // System.out.println(xml); 

    java.util.Map instancemap = new java.util.HashMap(); // String --> Vector 
    java.util.Map idmap = new java.util.HashMap();       // String --> String
    Vector entcodes = new Vector(); 

    for (int i = 0; i < entities.size(); i++) 
    { Entity et = (Entity) entities.get(i);
      String ename = et.getName().toLowerCase() + "s";  
      instancemap.put(ename,new Vector()); 
      entcodes.add(ename); 
    } 

    Vector enodes = xml.getSubnodes(); // all instances
    for (int i = 0; i < enodes.size(); i++) 
    { XMLNode enode = (XMLNode) enodes.get(i); 
      String cname = enode.getTag(); 
      Vector einstances = (Vector) instancemap.get(cname); 
      if (einstances == null) 
      { einstances = (Vector) instancemap.get(cname + "s"); } // For multiplicity ONE globally
      if (einstances != null) 
      { einstances.add(enode); }  
    } 
      
    for (int j = 0; j < entities.size(); j++) 
    { Entity et = (Entity) entities.get(j); 
      String ename = (String) entcodes.get(j); 
      Vector elems = (Vector) instancemap.get(ename); 
      for (int k = 0; k < elems.size(); k++) 
      { XMLNode enode = (XMLNode) elems.get(k);
        String tname = enode.getAttributeValue("xsi:type"); 
        if (tname == null) 
        { tname = et.getName(); } 
        else 
        { int colonind = tname.indexOf(":"); 
          if (colonind >= 0)
          { tname = tname.substring(colonind + 1,tname.length()); }
        }   
        pwout.println(ename + "" + k + " : " + tname); 

        String idval = enode.getAttributeValue("xmi:id");
        if (idval != null) 
        { idmap.put(idval,ename + "" + k); }  
      } 
    } 

    for (int j = 0; j < entities.size(); j++) 
    { Entity et = (Entity) entities.get(j); 
      String ename = (String) entcodes.get(j); 
      Vector elems = (Vector) instancemap.get(ename); 
      for (int k = 0; k < elems.size(); k++) 
      { XMLNode enode = (XMLNode) elems.get(k);
        Vector atts = enode.getAttributes();
        for (int p = 0; p < atts.size(); p++) 
        { XMLAttribute patt = (XMLAttribute) atts.get(p); 
          if (patt.getName().equals("xsi:type") || patt.getName().equals("xmi:id")) { } 
          else 
          { patt.getDataDeclaration(pwout,ename + k, et, idmap); } 
        } 
      }  
    } 
    try { pwout.close(); } catch (Exception ee) { } 
  }

  public String generateLoadXsiOp()
  { String res = "  public static void loadXSI()\n" + 
      "  { boolean __eof = false;\n" + 
      "    String __s = \"\";\n" + 
      "    String xmlstring = \"\";\n" +  
      "    BufferedReader __br = null;\n" + 
      "    try\n" + 
      "    { File _classmodel = new File(\"in.xmi\");\n" + 
      "      __br = new BufferedReader(new FileReader(_classmodel));\n" + 
      "      __eof = false;\n" + 
      "      while (!__eof)\n" + 
      "      { try { __s = __br.readLine(); }\n" + 
      "        catch (IOException __e)\n" + 
      "        { System.out.println(\"Reading failed.\");\n" + 
      "          return;\n" + 
      "        }\n" + 
      "        if (__s == null)\n" + 
      "        { __eof = true; }\n" + 
      "        else\n" + 
      "        { xmlstring = xmlstring + __s; }\n" +  
      "      } \n" + 
      "      __br.close();\n" +  
      "    } \n" + 
      "    catch (Exception _x) { }\n" +  
      "    Vector res = convertXsiToVector(xmlstring);\n" + 
      "    File outfile = new File(\"_in.txt\");\n" +  
      "    PrintWriter out; \n" + 
      "    try { out = new PrintWriter(new BufferedWriter(new FileWriter(outfile))); }\n" + 
      "    catch (Exception e) { return; } \n" + 
      "    for (int i = 0; i < res.size(); i++)\n" +  
      "    { String r = (String) res.get(i); \n" + 
      "      out.println(r);\n" + 
      "    } \n" + 
      "    out.close();\n" +  
      "    loadModel(\"_in.txt\");\n" + 
      "  }\n"; 
    return res; 
  }  


  public String generateLoadFromXsiOp()
  { String res = "  public static Vector convertXsiToVector(String xmlstring)\n" +
      "  { Vector res = new Vector();\n" +  
      "    XMLParser comp = new XMLParser();\n" + 
      "    comp.nospacelexicalanalysisxml(xmlstring);\n" + 
      "    XMLNode xml = comp.parseXML();\n" + 
      "    if (xml == null) { return res; } \n" +
      "    java.util.Map instancemap = new java.util.HashMap(); // String --> Vector\n" + 
      "    java.util.Map entmap = new java.util.HashMap();       // String --> String\n" +
      "    Vector entcodes = new Vector(); \n" +
      "    java.util.Map allattsmap = new java.util.HashMap(); // String --> Vector\n" + 
      "    java.util.Map stringattsmap = new java.util.HashMap(); // String --> Vector\n" +      
      "    java.util.Map onerolesmap = new java.util.HashMap(); // String --> Vector\n" +
      "    java.util.Map actualtype = new java.util.HashMap(); // XMLNode --> String\n" +
      "    Vector eallatts = new Vector();\n";

    for (int i = 0; i < entities.size(); i++)
    { Entity en = (Entity) entities.get(i);
      Vector allattnames = en.allAttributeNames();
      Vector allstringattnames = en.allStringAttributeNames();
      Vector allonerolenames = en.allOneRoleNames();
      String ename = en.getName();
      String enname = ename.toLowerCase();
      String esname = enname + "s";
      res = res +
      "    instancemap.put(\"" + esname + "\", new Vector()); \n" +
      "    instancemap.put(\"" + enname + "\",new Vector()); \n" +
      "    entcodes.add(\"" + esname + "\");\n" +    
      "    entcodes.add(\"" + enname + "\");\n" + 
      "    entmap.put(\"" + esname + "\",\"" + ename + "\");\n" + 
      "    entmap.put(\"" + enname + "\",\"" + ename + "\");\n" +
      "    eallatts = new Vector();\n";

      for (int j = 0; j < allattnames.size(); j++)
      { String aname = (String) allattnames.get(j);
        res = res +
          "    eallatts.add(\"" + aname + "\");\n";
      }
      res = res + 
          "    allattsmap.put(\"" + ename + "\", eallatts);\n";
     
      res = res + 
          "    eallatts = new Vector();\n";
      for (int j = 0; j < allstringattnames.size(); j++)
      { String aname = (String) allstringattnames.get(j);
        res = res +
          "    eallatts.add(\"" + aname + "\");\n";
      }
      res = res + 
          "    stringattsmap.put(\"" + ename + "\", eallatts);\n";
      res = res + 
          "    eallatts = new Vector();\n";
      for (int j = 0; j < allonerolenames.size(); j++)
      { String aname = (String) allonerolenames.get(j);
        res = res +
          "    eallatts.add(\"" + aname + "\");\n";
      }
      res = res + 
          "    onerolesmap.put(\"" + ename + "\", eallatts);\n";
    }
    res = res + 
          "    eallatts = new Vector();\n";

    res = res +
    "  Vector enodes = xml.getSubnodes();\n" +
    "  for (int i = 0; i < enodes.size(); i++)\n" + 
    "  { XMLNode enode = (XMLNode) enodes.get(i);\n" + 
    "    String cname = enode.getTag();\n" + 
    "    Vector einstances = (Vector) instancemap.get(cname); \n" +
    "    if (einstances == null) \n" +
    "    { einstances = (Vector) instancemap.get(cname + \"s\"); }\n" +
    "    if (einstances != null) \n" +
    "    { einstances.add(enode); }\n" +
    "  }\n" + 
    "  for (int j = 0; j < entcodes.size(); j++)\n" + 
    "  { String ename = (String) entcodes.get(j);\n" + 
    "    Vector elems = (Vector) instancemap.get(ename);\n" + 
    "    for (int k = 0; k < elems.size(); k++)\n" + 
    "    { XMLNode enode = (XMLNode) elems.get(k);\n" +
    "      String tname = enode.getAttributeValue(\"xsi:type\"); \n" +
    "      if (tname == null) \n" +
    "      { tname = (String) entmap.get(ename); } \n" +
    "      else \n" +
    "      { int colonind = tname.indexOf(\":\"); \n" +
    "        if (colonind >= 0)\n" +
    "        { tname = tname.substring(colonind + 1,tname.length()); }\n" +
    "      }\n" + 
    "      res.add(ename + k + \" : \" + tname);\n" +
    "      actualtype.put(enode,tname);\n" +  
    "    }   \n" + 
    "  }\n"; 

        // String idval = enode.getAttributeValue("xmi:id");
        // if (idval != null) 
        // { idmap.put(idval,ename + "" + k); }

  res = res +
    "  for (int j = 0; j < entcodes.size(); j++) \n" +
    "  { String ename = (String) entcodes.get(j); \n" +
    "    Vector elems = (Vector) instancemap.get(ename); \n" +
    "    for (int k = 0; k < elems.size(); k++)\n" +
    "    { XMLNode enode = (XMLNode) elems.get(k);\n" +
    "      String tname = (String) actualtype.get(enode);\n" +
    "      Vector tallatts = (Vector)  allattsmap.get(tname);\n" +
    "      Vector tstringatts = (Vector)  stringattsmap.get(tname);\n" +
    "      Vector toneroles = (Vector)  onerolesmap.get(tname);\n" +      
    "      Vector atts = enode.getAttributes();\n" +
    "      for (int p = 0; p < atts.size(); p++) \n" +
    "      { XMLAttribute patt = (XMLAttribute) atts.get(p); \n" +
    "        if (patt.getName().equals(\"xsi:type\") || patt.getName().equals(\"xmi:id\")) {} \n" +
    "        else \n" +
    "        { patt.getDataDeclarationFromXsi(res,tallatts,tstringatts,toneroles,ename + k, (String) entmap.get(ename)); } \n" +
    "      }\n" + 
    "    } \n" +
    "  }  \n" +
    "  return res; } \n";
    return res; 
  }

  public void convertXsiToData2()
  { BufferedReader br = null;
    BufferedWriter brout = null; 
    PrintWriter pwout = null; 

    Vector res = new Vector();
    String s;
    boolean eof = false;
    File infile = new File("output/xsi.txt");  /* default */ 
    File outfile = new File("output/model.txt"); 

    try
    { br = new BufferedReader(new FileReader(infile));
      brout = new BufferedWriter(new FileWriter(outfile)); 
      pwout = new PrintWriter(brout); 
    }
    catch (Exception e)
    { System.out.println("Errors with files: " + infile + " " + outfile);
      return; 
    }

    Vector enodes = new Vector(); // all instances

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else 
      { Compiler2 comp = new Compiler2();  
        comp.nospacelexicalanalysisxml(s); 
        XMLNode xml = comp.parseXMLNode(); 
        if (xml != null) 
        { enodes.add(xml); } 
      }
    }

    // System.out.println(xml); 

    java.util.Map instancemap = new java.util.HashMap(); // String --> Vector 
    java.util.Map idmap = new java.util.HashMap();       // String --> String
    Vector entcodes = new Vector(); 

    for (int i = 0; i < entities.size(); i++) 
    { Entity et = (Entity) entities.get(i);
      String ename = et.getName().toLowerCase() + "s";  
      instancemap.put(ename,new Vector()); 
      entcodes.add(ename); 
    } 

    for (int i = 0; i < enodes.size(); i++) 
    { XMLNode enode = (XMLNode) enodes.get(i); 
      String cname = enode.getTag(); 
      Vector einstances = (Vector) instancemap.get(cname); 
      if (einstances == null) 
      { einstances = (Vector) instancemap.get(cname + "s"); }
      if (einstances != null) 
      { einstances.add(enode); }  
    } 
      
    for (int j = 0; j < entities.size(); j++) 
    { Entity et = (Entity) entities.get(j); 
      String ename = (String) entcodes.get(j); 
      Vector elems = (Vector) instancemap.get(ename); 
      for (int k = 0; k < elems.size(); k++) 
      { XMLNode enode = (XMLNode) elems.get(k);
        String tname = enode.getAttributeValue("xsi:type"); 
        if (tname == null) 
        { tname = et.getName(); } 
        else 
        { int colonind = tname.indexOf(":"); 
          if (colonind >= 0)
          { tname = tname.substring(colonind + 1,tname.length()); }
        }   
        pwout.println(ename + "" + k + " : " + tname); 

        String idval = enode.getAttributeValue("xmi:id");
        if (idval != null) 
        { idmap.put(idval,ename + "" + k); }  
      } 
    } 

    for (int j = 0; j < entities.size(); j++) 
    { Entity et = (Entity) entities.get(j); 
      String ename = (String) entcodes.get(j); 
      Vector elems = (Vector) instancemap.get(ename); 
      for (int k = 0; k < elems.size(); k++) 
      { XMLNode enode = (XMLNode) elems.get(k);
        Vector atts = enode.getAttributes();
        for (int p = 0; p < atts.size(); p++) 
        { XMLAttribute patt = (XMLAttribute) atts.get(p); 
          if (patt.getName().equals("xsi:type") || patt.getName().equals("xmi:id")) { } 
          else 
          { patt.getDataDeclaration(pwout, ename + k, et, idmap); } 
        } 
      }  
    } 
    try { pwout.close(); } catch (Exception ee) { } 
  }

  public void loadEcoreFromFile()
  { BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/mm.ecore");  /* default */ 

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException e)
    { System.out.println("File not found: " + file);
      return; 
    }

    Vector preentities = new Vector(); 
    Vector preassociations = new Vector(); 
    Vector pregeneralisations = new Vector();
    Vector preconstraints = new Vector(); 
    Vector preassertions = new Vector(); 
    Vector preops = new Vector(); 
    Vector pucs = new Vector(); 
    Vector preactivities = new Vector(); 
    Vector preucinvs = new Vector(); 

    String xmlstring = ""; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println(">>> Reading mm.ecore failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else 
      { xmlstring = xmlstring + s + " "; } 
    }

    Compiler2 comp = new Compiler2();  
    comp.nospacelexicalanalysisxml(xmlstring); 
    XMLNode xml = comp.parseXML(); 
    System.out.println(">>> Parsed ecore data: " + xml); 

    Vector enodes = xml.getSubnodes(); // entities and types
    int delta = 200; // visual displacement 
    int ecount = 0; 

    for (int i = 0; i < enodes.size(); i++) 
    { XMLNode enode = (XMLNode) enodes.get(i); 
      if ("eClassifiers".equals(enode.getTag()))
      { String xsitype = enode.getAttributeValue("xsi:type"); 
        String ename = enode.getAttributeValue("name"); 
        if ("ecore:EClass".equals(xsitype) && ename != null)  
        { Entity ent = 
            reconstructEntity(ename, 40 + (ecount/3)*delta + ((ecount % 3)*delta)/2, 
                              100 + (ecount % 7)*delta, "", "*", new Vector());
          ecount++; 
        } 
        else if ("ecore:EEnum".equals(xsitype) && ename != null)
        { Vector eliterals = new Vector(); 
          Vector esubs = enode.getSubnodes(); 
          for (int k = 0; k < esubs.size(); k++) 
          { XMLNode esb = (XMLNode) esubs.get(k); 
            if ("eLiterals".equals(esb.getTag()))
            { eliterals.add(esb.getAttributeValue("name")); } 
          } 
          Type tt = new Type(ename,eliterals); 
          types.add(tt); 
          RectData rd = new RectData(100 + 100*types.size(),20,getForeground(),
                                 componentMode,
                                 rectcount);
          rectcount++;
          rd.setLabel(ename);
          rd.setModelElement(tt); 
          visuals.add(rd); 
        } 
      } 
    } 

    for (int i = 0; i < enodes.size(); i++) 
    { XMLNode enode = (XMLNode) enodes.get(i); 
      if ("eClassifiers".equals(enode.getTag()))
      { String xsitype = enode.getAttributeValue("xsi:type"); 
        String ename = enode.getAttributeValue("name"); 
        if ("ecore:EClass".equals(xsitype) && ename != null)  
        { Entity ent = (Entity) ModelElement.lookupByName(ename,entities);
          String esupers = enode.getAttributeValue("eSuperTypes"); 
          if (esupers != null && esupers.startsWith("#//"))
          { String[] allsupers = esupers.split(" ");
            for (int p = 0; p < allsupers.length; p++) 
            { String supr = (String) allsupers[p];
              String suprname = supr.substring(3,supr.length());   
              Entity supent = (Entity) ModelElement.lookupByName(suprname,entities);
            
            // String supername = esupers.substring(3,esupers.length()); 
            // Entity supent = (Entity) ModelElement.lookupByName(supername,entities);
              if (supent != null) 
              { Entity[] subents = new Entity[1]; 
                subents[0] = ent; 
                addInheritances(supent,subents); 
                supent.setAbstract(true);
              } 
            } 
          } 

          Vector edata = enode.getSubnodes(); 
          for (int j = 0; j < edata.size(); j++) 
          { XMLNode ed = (XMLNode) edata.get(j); 
            if ("eStructuralFeatures".equals(ed.getTag()))
            { String dataname = ed.getAttributeValue("name"); 
              if ("ecore:EAttribute".equals(ed.getAttributeValue("xsi:type")))
              { Type typ = Type.getEcoreType(ed.getAttributeValue("eType"),types); 
                Attribute att = new Attribute(dataname,typ,ModelElement.INTERNAL); 
                ent.addAttribute(att); 
                att.setEntity(ent); 
              } 
              else if ("ecore:EReference".equals(ed.getAttributeValue("xsi:type")))
              { String e2name = ed.getAttributeValue("eType"); 
                e2name = e2name.substring(3,e2name.length()); 
                Entity ent2 = (Entity) ModelElement.lookupByName(e2name,entities);
                String upper = ed.getAttributeValue("upperBound"); // default is 1
                String lower = ed.getAttributeValue("lowerBound"); // default is 0 
                String opposite = ed.getAttributeValue("eOpposite"); 
                String rolename = ed.getAttributeValue("name"); 
                String containment = ed.getAttributeValue("containment"); 

                if ("-1".equals(upper)) { } 
                else 
                { upper = "1"; } 
                if ("1".equals(lower)) { } 
                else 
                { lower = "0"; } 

                System.out.println(">> Association: " + rolename + " from " + ename + " to " + e2name); 
                System.out.println(">> Opposite is " + opposite); 
                String opp = opposite; 

                if (ent2 != null && opposite != null && opposite.startsWith("#//"))
                { opp = opposite.substring(3,opposite.length()); // E1/role1
                  int ind = opp.indexOf("/"); 
                  String e1name = opp.substring(0,ind); 
                  String role1 = opp.substring(ind+1,opp.length()); 
                  Association oldast = ent2.getRole(role1);
                  System.out.println(">>> " + e2name + " --" + role1 + "-> " + ename);  

                  if (oldast != null) 
                  { oldast.setRole1(rolename); 
                    oldast.setCard1(lower,upper);
                    continue;  
                  } 
                } 

             
                Association ast = new Association(ent,ent2,lower,upper,opp,
                                                  rolename);
                if ("true".equals(containment)) 
                { ast.setAggregation(true); 
                  // ast.setCard1(ModelElement.ZEROONE); 
                } 

                associations.add(ast);  
                ent.addAssociation(ast); 
                ast.setName("r" + associations.size());
                int xs = 0, ys = 0, xe = 100, ye = 100;  
                for (int m = 0; m < visuals.size(); m++)
                { VisualData vd = (VisualData) visuals.get(m); 
                  ModelElement me = (ModelElement) vd.getModelElement(); 
                  if (me == ent) // Entity1
                  { xs = vd.getx(); ys = vd.gety(); } 
                  else if (me == ent2) // Entity2
                  { xe = vd.getx(); ye = vd.gety(); }  
                }

                int featuresize = ent.featureCount(); 
                int efeaturesize = ent2.featureCount(); 

                LineData sline = 
                  new LineData(xs + featuresize*4, ys+50, xe + efeaturesize*4,ye,linecount,SOLID);
                sline.setModelElement(ast); 
                visuals.add(sline); 
              }
            }
          }
        } 
      }
    }

    repaint(); 
  }

  public void loadKM3FromFile()
  { BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/mm.km3");  /* default */ 

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException e)
    { System.out.println("File not found: " + file);
      return; 
    }


    String xmlstring = ""; 
    int linecount = 0; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else if (s.startsWith("--")) { } 
      else 
      { xmlstring = xmlstring + s + " "; } 
      linecount++; 
    }

    Vector pregens = new Vector(); 
    Vector preassocs = new Vector(); 

    Compiler2 comp = new Compiler2();  
    comp.nospacelexicalanalysis(xmlstring); 
    Vector items = comp.parseKM3(entities,types,pregens,preassocs); 
    System.out.println(linecount + " lines in file mm.km3"); 

    Vector passocs = new Vector(); 
    passocs.addAll(preassocs); 

    for (int aa = 0; aa < preassocs.size(); aa++) 
    { PreAssociation a1 = (PreAssociation) preassocs.get(aa); 
      for (int bb = aa+1; bb < preassocs.size(); bb++) 
      { PreAssociation b1 = (PreAssociation) preassocs.get(bb); 
        if (a1.isDualTo(b1)) 
        { PreAssociation c1 = a1.combineWith(b1); 
          passocs.remove(a1); 
          passocs.remove(b1); 
          passocs.add(c1); 
        } 
      } 
    } 

    int delta = 80; // visual displacement 
    int ecount = 0; 

    for (int i = 0; i < entities.size(); i++) 
    { Entity enode = (Entity) entities.get(i); 
      addEntity(enode, 20 + (ecount/5)*delta + ((ecount % 5)*delta)/5, 
                              100 + (ecount % 5)*delta);
      ecount++; 
    } 

    for (int j = 0; j < types.size(); j++) 
    { Type tt = (Type) types.get(j); 
      if (tt.isEnumeration())
      { RectData rd = new RectData(100*j,20,getForeground(),
                                 componentMode,
                                 rectcount);
        rectcount++;
        rd.setLabel(tt.getName());
        rd.setModelElement(tt); 
        visuals.add(rd); 
      } 
    } 

    for (int p = 0; p < pregens.size(); p++) 
    { PreGeneralisation pg = (PreGeneralisation) pregens.get(p); 
      String e1n = pg.e1name;   // subclass
      String e2n = pg.e2name;   // superclass 
      Entity subc = (Entity) ModelElement.lookupByName(e1n,entities);
      Entity supc = (Entity) ModelElement.lookupByName(e2n,entities);
      if (subc != null && supc != null) 
      { Generalisation g = new Generalisation(supc,subc);
        addInheritance(g,supc,subc); 
        supc.setAbstract(true); 
      } 
    } 

    for (int q = 0; q < passocs.size(); q++) 
    { PreAssociation pa = (PreAssociation) passocs.get(q);  
      Entity e1 =
        (Entity) ModelElement.lookupByName(pa.e1name,entities);
      Entity e2 =
        (Entity) ModelElement.lookupByName(pa.e2name,entities);
      
      if (e1 != null && e2 != null) 
      { RectData rd1 = (RectData) getVisualOf(e1); 
        RectData rd2 = (RectData) getVisualOf(e2);
        int xs = rd1.sourcex + 50;
        int ys = rd1.sourcey + 50; 
        int xe = rd2.sourcex; 
        int ye = rd2.sourcey + 50;   

        reconstructAssociation(pa.e1name,pa.e2name,xs,ys,
                             xe,ye,pa.card1,pa.card2,
                             pa.role2, pa.role1, pa.stereotypes, new Vector());
      } 
    } 

    for (int h = 0; h < items.size(); h++) 
    { Object hx = items.get(h); 
      if (hx instanceof UseCase) 
      { addGeneralUseCase((UseCase) hx); } 
    } 

    repaint(); 
  }

  public void processKM3(Vector ents, Vector typs, Vector pregens, Vector preassocs, Vector items) 
  { Vector oldentities = new Vector(); 
    oldentities.addAll(entities); 
    entities.clear(); 
    
    Vector oldtypes = new Vector(); 
    oldtypes.addAll(types); 
    types.clear(); 
    
    Vector olduseCases = new Vector();
    olduseCases.addAll(useCases);  
    useCases.clear(); 

    Vector oldVisuals = new Vector(); 
    oldVisuals.addAll(visuals); 

    repaint(); 

    Vector passocs = new Vector(); 
    passocs.addAll(preassocs); 

    for (int aa = 0; aa < preassocs.size(); aa++) 
    { PreAssociation a1 = (PreAssociation) preassocs.get(aa); 
      for (int bb = aa+1; bb < preassocs.size(); bb++) 
      { PreAssociation b1 = (PreAssociation) preassocs.get(bb); 
        if (a1.isDualTo(b1)) 
        { PreAssociation c1 = a1.combineWith(b1); 
          passocs.remove(a1); 
          passocs.remove(b1); 
          passocs.add(c1); 
        } 
      } 
    } 

    int delta = 80; // visual displacement 
    int ecount = 0; 

    // Use the existing coordinates if possible. 

    for (int i = 0; i < ents.size(); i++) 
    { Entity enode = (Entity) ents.get(i); 
      Entity oldent = (Entity) ModelElement.lookupByName(enode.getName(), oldentities); 
      if (oldent != null) 
      { RectData rd = (RectData) getVisualOf(oldent); 
        entities.add(enode); 
        rd.setModelElement(enode); 
        oldVisuals.remove(rd); 
        // addEntity(enode, rd.sourcex, rd.sourcey); 
      } 
      else 
      { addEntity(enode, 20 + (ecount/5)*delta + ((ecount % 5)*delta)/5, 
                              100 + (ecount % 5)*delta);
      } 
      ecount++; 
    } 

    for (int j = 0; j < typs.size(); j++) 
    { Type tt = (Type) typs.get(j); 
      if (tt.isEnumeration())
      { RectData rd = new RectData(100*j,20,getForeground(),
                                 componentMode,
                                 rectcount);
        rectcount++;
        rd.setLabel(tt.getName());
        rd.setModelElement(tt); 
        visuals.add(rd); 
      } 
    } // preserve the existing one if it exists

    for (int p = 0; p < pregens.size(); p++) 
    { PreGeneralisation pg = (PreGeneralisation) pregens.get(p); 
      String e1n = pg.e1name;   // subclass
      String e2n = pg.e2name;   // superclass 
      Entity subc = (Entity) ModelElement.lookupByName(e1n,ents);
      Entity supc = (Entity) ModelElement.lookupByName(e2n,ents);
      if (subc != null && supc != null) 
      { Generalisation oldgen = lookupGeneralisation(subc, supc); 
        if (oldgen != null) 
        { LineData ld = (LineData) getVisualOf(oldgen); 
          oldVisuals.remove(ld);
          subc.setSuperclass(supc); 
          supc.addSubclass(subc); 
        }  
        else 
        { Generalisation g = new Generalisation(supc,subc);
          addInheritance(g,supc,subc); 
          supc.setAbstract(true);
        }  
      } 
    } 

    for (int q = 0; q < passocs.size(); q++) 
    { PreAssociation pa = (PreAssociation) passocs.get(q);  
      Entity e1 =
        (Entity) ModelElement.lookupByName(pa.e1name,ents);
      Entity e2 =
        (Entity) ModelElement.lookupByName(pa.e2name,ents);
      
      if (e1 != null && e2 != null) 
      { Association ast = lookupAssociation(e1,e2,pa.role2); 
        if (ast != null) // already in associations
        { LineData ld = (LineData) getVisualOf(ast); 
          oldVisuals.remove(ld); 
          ast.updateAssociation(pa.card1, pa.card2, pa.role1, pa.role2, pa.stereotypes); 
          ast.setEntity1(e1); 
          ast.setEntity2(e2); 
          e1.addAssociation(ast);
        } 
        else 
        { RectData rd1 = (RectData) getVisualOf(e1); 
          RectData rd2 = (RectData) getVisualOf(e2);
          int xs = rd1.sourcex + 10;
          int ys = rd1.sourcey + 10; 
          int xe = rd2.sourcex + 10; 
          int ye = rd2.sourcey + 10;   

          reconstructAssociation(pa.e1name,pa.e2name,xs,ys,
                             xe,ye,pa.card1,pa.card2,
                             pa.role2, pa.role1, pa.stereotypes, new Vector());
        } 
      } 
    } 

    for (int h = 0; h < items.size(); h++) 
    { Object hx = items.get(h); 
      if (hx instanceof UseCase) 
      { addGeneralUseCase((UseCase) hx); } 
    } 

    visuals.removeAll(oldVisuals); 

    repaint(); 
  }

  public void loadModelFromFile()
  { BufferedReader br = null;
    Vector res = new Vector();
    String s;
    boolean eof = false;
    File file = new File("output/model.txt");  /* default */ 

    try
    { br = new BufferedReader(new FileReader(file)); }
    catch (FileNotFoundException e)
    { System.out.println("File not found: " + file);
      return; 
    }

    java.util.Map preentities = new java.util.HashMap(); 
    java.util.Map preproperties = new java.util.HashMap(); 
    // Vector preassociations = new Vector(); 
    java.util.Map pregeneralisations = new java.util.HashMap();
    java.util.Map preconstraints = new java.util.HashMap(); 
    Vector preassertions = new Vector(); 
    java.util.Map preops = new java.util.HashMap(); 
    java.util.Map preucs = new java.util.HashMap(); 
    java.util.Map preexps = new java.util.HashMap(); 
    Vector preactivities = new Vector(); 
    Vector preucinvs = new Vector(); 

    /* Also process PrimitiveType, CollectionType */ 

    String str = ""; 
    int delta = 80; // visual displacement 
    int ecount = 0; 

    while (!eof)
    { try { s = br.readLine(); }
      catch (IOException e)
      { System.out.println("Reading failed.");
        return; 
      }
      if (s == null) 
      { eof = true; 
        break; 
      }
      else if (s.startsWith("--")) { } 
      else 
      { str = s.trim();
        String[] strs = str.split(" "); 
        if (str.endsWith("Entity"))
        { String ename = strs[0]; 
          Entity e =         
            reconstructEntity(ename, 20 + (ecount/5)*delta + ((ecount % 5)*delta)/5, 
                              100 + (ecount % 5)*delta, "", "*", new Vector());
          ecount++; 
          preentities.put(ename,e); 
          // entities.add(e); 
        }
        else if (str.endsWith("Property"))
        { String alabel = strs[0]; 
          preproperties.put(alabel, new PreProperty(alabel)); 
        } 
        else if (str.endsWith("Generalization"))
        { String glabel = strs[0]; 
          Generalisation g = new Generalisation(glabel); 
          pregeneralisations.put(glabel,g); 
        } 
        else if (str.endsWith("BehaviouralFeature") || 
                 (str.endsWith("Operation") && !str.endsWith("ownedOperation")))
        { String glabel = strs[0]; 
          BehaviouralFeature op = new BehaviouralFeature(glabel);
          // System.out.println("NEW OPERATION " + glabel);  
          preops.put(glabel,op); 
        } 
        else if (str.endsWith("UseCase"))
        { String glabel = strs[0]; 
          UseCase uc = new UseCase(glabel); 
          preucs.put(glabel,uc); 
        } 
        else if (str.endsWith("Constraint"))
        { String glabel = strs[0]; 
          Constraint cc = new Constraint(); 
          preconstraints.put(glabel,cc); 
        } 
        else if (str.endsWith("BasicExpression"))
        { String glabel = strs[0]; 
          BasicExpression be = new BasicExpression(glabel); 
          preexps.put(glabel,be); 
        } 
        else if (str.endsWith("BinaryExpression"))
        { String glabel = strs[0]; 
          BinaryExpression be = new BinaryExpression(glabel,null,null); 
          preexps.put(glabel,be); 
        } 
        else if (str.endsWith("UnaryExpression"))
        { String glabel = strs[0]; 
          UnaryExpression be = new UnaryExpression(glabel,null); 
          preexps.put(glabel,be); 
        } 
        else if (str.endsWith("SetExpression"))
        { String glabel = strs[0]; 
          SetExpression se = new SetExpression(); 
          preexps.put(glabel,se); 
        } // and process its elements/ordering
        else if (strs.length > 1 && "=".equals(strs[1]))  // x.prop = val
        { String lft = strs[0];
          int idx = lft.indexOf(".");
          if (idx > 0)
          { String x = lft.substring(0,idx);
            String prop = lft.substring(idx+1,lft.length());
            int ind2 = str.indexOf("="); 
            String val = str.substring(ind2 + 1, str.length());
            val = val.trim(); 
            // System.out.println("LINE: " + x + " . " + prop + " = " + val); 

            if ("type".equals(prop))
            { if (preproperties.keySet().contains(x))
              { PreProperty pp = (PreProperty) preproperties.get(x);
                if (preentities.keySet().contains(val))
                { Entity e2 = (Entity) preentities.get(val);
                  pp.setEntity2(e2);
                }
                else // if (Type.isAttributeType(val))
                { Type typ = ModelElement.model2type(val); 
                  pp.setType(typ);
                }
              }
              else if (preops.keySet().contains(x))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(x); 
                bf.setResultType(ModelElement.model2type(val)); 
              } 
              else if (preexps.keySet().contains(x))
              { Expression ee = (Expression) preexps.get(x); 
                ee.setType(ModelElement.model2type(val)); 
              } 
            }
            else if ("elementType".equals(prop))
            { if (preproperties.keySet().contains(x))
              { PreProperty pp = (PreProperty) preproperties.get(x);
                if (preentities.keySet().contains(val))
                { Entity e2 = (Entity) preentities.get(val);
                  pp.setEntity2(e2);
                }
                else // if (Type.isAttributeType(val))
                { Type typ = ModelElement.model2type(val); 
                  pp.setElementType(typ);
                }
              }
              else if (preops.keySet().contains(x))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(x); 
                bf.setElementType(ModelElement.model2type(val)); 
              } 
              else if (preexps.keySet().contains(x))
              { Expression ee = (Expression) preexps.get(x); 
                ee.setElementType(ModelElement.model2type(val)); 
              } 
            }
            else if ("name".equals(prop))
            { if (preproperties.keySet().contains(x))
              { PreProperty pp = (PreProperty) preproperties.get(x);
                pp.setName(ModelElement.destring(val));
              }
              else if (preops.keySet().contains(x))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(x); 
                bf.setName(ModelElement.destring(val)); 
              } 
              else if (preucs.keySet().contains(x))
              { UseCase uc = (UseCase) preucs.get(x); 
                uc.setName(ModelElement.destring(val)); 
                if (useCases.contains(uc)) { } 
                else 
                { addGeneralUseCase(uc); }  
              } 
            }
            else if ("isStatic".equals(prop))
            { if (preops.keySet().contains(x) && "true".equals(val))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(x); 
                bf.setInstanceScope(false); 
              }  // or an attribute can be static
            } 
            else if ("isQuery".equals(prop))
            { if (preops.keySet().contains(x) && "true".equals(val))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(x); 
                bf.setQuery(true); 
              }  
            } 
            else if ("precondition".equals(prop))
            { if (preops.keySet().contains(x) && preexps.keySet().contains(val))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(x); 
                bf.setPre((Expression) preexps.get(val)); 
              }  
            } 
            else if ("postcondition".equals(prop))
            { if (preops.keySet().contains(x) && preexps.keySet().contains(val))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(x); 
                bf.setPost((Expression) preexps.get(val)); 
              }  
            } 
            else if ("specific".equals(prop))
            { if (pregeneralisations.keySet().contains(x))
              { Generalisation g = (Generalisation) pregeneralisations.get(x); 
                if (preentities.keySet().contains(val))
                { Entity sp = (Entity) preentities.get(val); 
                  g.setDescendent(sp); 
                } 
              } 
            } 
            else if ("general".equals(prop))
            { if (pregeneralisations.keySet().contains(x))
              { Generalisation g = (Generalisation) pregeneralisations.get(x); 
                if (preentities.keySet().contains(val))
                { Entity sp = (Entity) preentities.get(val); 
                  g.setAncestor(sp); 
                  sp.setAbstract(true); 
                } 
              } 
            } 
            else if ("data".equals(prop))
            { if (preexps.keySet().contains(x))
              { BasicExpression be = (BasicExpression) preexps.get(x); 
                be.setData(ModelElement.destring(val));  
              } 
            } 
            else if ("prestate".equals(prop))
            { if (preexps.keySet().contains(x))
              { BasicExpression be = (BasicExpression) preexps.get(x); 
                if ("true".equals(val)) { be.setPrestate(true); }   
              } 
            } 
            else if ("operator".equals(prop))
            { if (preexps.keySet().contains(x))
              { Expression ee = (Expression) preexps.get(x); 
                if (ee instanceof BinaryExpression)
                { ((BinaryExpression) ee).setOperator(ModelElement.destring(val)); }
                else if (ee instanceof UnaryExpression)
                { ((UnaryExpression) ee).setOperator(ModelElement.destring(val)); }    
              } 
            } 
            else if ("left".equals(prop))
            { if (preexps.keySet().contains(x))
              { Expression ee = (Expression) preexps.get(x); 
                if (preexps.keySet().contains(val))
                { Expression ex = (Expression) preexps.get(val); 
                  if (ee instanceof BinaryExpression)
                  { ((BinaryExpression) ee).setLeft(ex); }
                } 
              } 
            }
            else if ("right".equals(prop))
            { if (preexps.keySet().contains(x))
              { Expression ee = (Expression) preexps.get(x); 
                if (preexps.keySet().contains(val))
                { Expression ex = (Expression) preexps.get(val); 
                  if (ee instanceof BinaryExpression)
                  { ((BinaryExpression) ee).setRight(ex); }
                } 
              } 
            }
            else if ("argument".equals(prop))
            { if (preexps.keySet().contains(x))
              { Expression ee = (Expression) preexps.get(x); 
                if (preexps.keySet().contains(val))
                { Expression ex = (Expression) preexps.get(val); 
                  if (ee instanceof UnaryExpression)
                  { ((UnaryExpression) ee).setArgument(ex); }
                } 
              } 
            }
            else if ("condition".equals(prop))
            { if (preconstraints.keySet().contains(x))
              { Constraint cc = (Constraint) preconstraints.get(x); 
                if (preexps.keySet().contains(val))
                { Expression ex = (Expression) preexps.get(val); 
                  cc.setAntecedent(ex); 
                } 
              } 
            }
            else if ("succedent".equals(prop))
            { if (preconstraints.keySet().contains(x))
              { Constraint cc = (Constraint) preconstraints.get(x); 
                if (preexps.keySet().contains(val))
                { Expression ex = (Expression) preexps.get(val); 
                  cc.setSuccedent(ex); 
                } 
              } 
            }
            else if ("owner".equals(prop))
            { if (preconstraints.keySet().contains(x))
              { Constraint cc = (Constraint) preconstraints.get(x); 
                if (preentities.keySet().contains(val))
                { Entity ee = (Entity) preentities.get(val); 
                  cc.setOwner(ee); 
                } 
              } 
            }
            else if ("usecase".equals(prop))
            { if (preconstraints.keySet().contains(x))
              { Constraint cc = (Constraint) preconstraints.get(x); 
                if (preucs.keySet().contains(val))
                { UseCase uu = (UseCase) preucs.get(val); 
                  cc.setUseCase(uu); 
                  uu.addPostcondition(cc); 
                } 
              } 
            }
          }
        }
        else if (strs.length > 1 && ":".equals(strs[1])) // x : val.prop
        { String rt = strs[2];
          int idx = rt.indexOf(".");
          if (idx > 0)
          { String val = rt.substring(0,idx);
            String prop = rt.substring(idx+1,rt.length());
            String x = strs[0];
            if ("ownedAttribute".equals(prop))
            { if (preentities.keySet().contains(val))
              { Entity e = (Entity) preentities.get(val);
                PreProperty pp = (PreProperty) preproperties.get(x);
                if (pp != null) { pp.setOwner(e); }
              }
            }
            else if ("ownedOperation".equals(prop))
            { // System.out.println("Adding operation " + x + " " + val); 
              if (preentities.keySet().contains(val))
              { Entity e = (Entity) preentities.get(val);
                BehaviouralFeature bf = (BehaviouralFeature) preops.get(x);
                // System.out.println("Adding operation " + bf + " " + e); 
                if (bf != null && e != null) 
                { bf.setEntity(e); 
                  e.addOperation(bf); 
                }
              }
            }
            else if ("parameters".equals(prop))
            { if (preops.keySet().contains(val))
              { BehaviouralFeature bf = (BehaviouralFeature) preops.get(val);
                PreProperty pp = (PreProperty) preproperties.get(x);
                if (pp != null) { pp.setOp(bf); }
              }
            }
            else if ("objectRef".equals(prop))
            { if (preexps.keySet().contains(val) && preexps.keySet().contains(x))
              { BasicExpression bx = (BasicExpression) preexps.get(val);
                Expression ee = (Expression) preexps.get(x);
                if (bx != null) { bx.setObjectRef(ee); }
              }
            }
          }
        }
      }
    }


    // System.out.println("Loaded from model.txt"); 


    java.util.Set preprops = preproperties.keySet();
    Vector preps = new Vector(); 
    preps.addAll(preprops); 

    for (int i = 0; i < preps.size(); i++)
    { String pname = (String) preps.get(i);
      PreProperty pp = (PreProperty) preproperties.get(pname);

      if (pp.name == null || pp.name.trim().length() == 0) 
      { continue; }  // valid features have a name

      Type ptyp = pp.type;
      if (pp.owner != null && ptyp != null && Type.isAttributeType(ptyp))
      { Attribute att = new Attribute(pp.name, ptyp, ModelElement.INTERNAL); 
        pp.owner.addAttribute(att);
        att.setEntity(pp.owner);
      }
      else if (pp.owner != null && pp.entity2 != null)
      { Association ast = new Association(pp.owner, pp.entity2, 
                                          ModelElement.MANY, ModelElement.ONE, null, pp.name);
        associations.add(ast);
        pp.owner.addAssociation(ast);
        int xs = 0, ys = 0, xe = 100, ye = 100;  
        for (int m = 0; m < visuals.size(); m++)
        { VisualData vd = (VisualData) visuals.get(m); 
          ModelElement me = (ModelElement) vd.getModelElement(); 
          if (me == pp.owner) // Entity1
          { xs = vd.getx(); ys = vd.gety(); } 
          else if (me == pp.entity2) // Entity2
          { xe = vd.getx(); ye = vd.gety(); }  
        }

        if (pp.owner == pp.entity2) 
        { xe = xs + 50; ye = ys + 40; } 

        LineData sline = 
          new LineData(xs,ys,xe,ye,linecount,SOLID);
        sline.setModelElement(ast); 
        visuals.add(sline); 
      }
      else if (pp.op != null) 
      { if (pp.type != null) 
        { Attribute att = new Attribute(pp.name, pp.type, ModelElement.INTERNAL); 
          pp.op.addParameter(att); 
        } 
        else if (pp.entity2 != null) 
        { Attribute att = new Attribute(pp.name, new Type(pp.entity2), ModelElement.INTERNAL); 
          pp.op.addParameter(att); 
        } 
      }  
    }

    Vector gens = new Vector(); 
    gens.addAll(pregeneralisations.keySet()); 

    for (int i = 0; i < gens.size(); i++) 
    { Generalisation g = (Generalisation) pregeneralisations.get(gens.get(i)); 
      Entity subent = g.getDescendent(); 
      Entity supent = g.getAncestor();  
      addInheritance(g,supent,subent); 
    } 
        
    Vector ees = new Vector(); 
    ees.addAll(preexps.keySet()); 

    for (int i = 0; i < ees.size(); i++) 
    { Expression e = (Expression) preexps.get(ees.get(i)); 
      System.out.println(e); 
    } 

    /* for (int i = 0; i < enodes.size(); i++) 
    { XMLNode enode = (XMLNode) enodes.get(i); 
      if ("eClassifiers".equals(enode.getTag()))
      { String xsitype = enode.getAttributeValue("xsi:type"); 
        String ename = enode.getAttributeValue("name"); 
        if ("ecore:EClass".equals(xsitype) && ename != null)  
        { Entity ent = (Entity) ModelElement.lookupByName(ename,entities);
          String esupers = enode.getAttributeValue("eSuperTypes"); 
          if (esupers != null && esupers.startsWith("#//"))
          { String supername = esupers.substring(3,esupers.length()); 
            Entity supent = (Entity) ModelElement.lookupByName(supername,entities);
            if (supent != null) 
            { Entity[] subents = new Entity[1]; 
              subents[0] = ent; 
              addInheritances(supent,subents); 
              supent.setAbstract(true); 
            } 
          } 
          Vector edata = enode.getSubnodes(); 
          for (int j = 0; j < edata.size(); j++) 
          { XMLNode ed = (XMLNode) edata.get(j); 
            if ("eStructuralFeatures".equals(ed.getTag()))
            { String dataname = ed.getAttributeValue("name"); 
              if ("ecore:EAttribute".equals(ed.getAttributeValue("xsi:type")))
              { Type typ = Type.getEcoreType(ed.getAttributeValue("eType"),types); 
                Attribute att = new Attribute(dataname,typ,ModelElement.INTERNAL); 
                ent.addAttribute(att); 
                att.setEntity(ent); 
              } 
              else if ("ecore:EReference".equals(ed.getAttributeValue("xsi:type")))
              { String e2name = ed.getAttributeValue("eType"); 
                e2name = e2name.substring(3,e2name.length()); 
                Entity ent2 = (Entity) ModelElement.lookupByName(e2name,entities);
                String upper = ed.getAttributeValue("upperBound"); 
                String lower = ed.getAttributeValue("lowerBound"); 
                String opposite = ed.getAttributeValue("eOpposite"); 

                System.out.println("Association: from " + ename + " to " + e2name); 
                System.out.println("Opposite is " + opposite); 
                String opp = opposite; 

                if (ent2 != null && opposite != null && opposite.startsWith("#//"))
                { opp = opposite.substring(3,opposite.length()); // E1/role1
                  int ind = opp.indexOf("/"); 
                  String e1name = opp.substring(0,ind); 
                  String role1 = opp.substring(ind+1,opp.length()); 
                  Association oldast = ent2.getRole(role1);
                  System.out.println(e1name + " " + role1 + " " + oldast);  

                  if (oldast != null) 
                  { oldast.setRole1(ed.getAttributeValue("name")); 
                    oldast.setCard1(lower,upper);
                    continue;  
                  } 
                } 

                Association ast = new Association(ent,ent2,lower,upper,opp,
                                        ed.getAttributeValue("name"));
                associations.add(ast);  
                ent.addAssociation(ast); 
                ast.setName("r" + associations.size());
                int xs = 0, ys = 0, xe = 100, ye = 100;  
                for (int m = 0; m < visuals.size(); m++)
                { VisualData vd = (VisualData) visuals.get(m); 
                  ModelElement me = (ModelElement) vd.getModelElement(); 
                  if (me == ent) // Entity1
                  { xs = vd.getx(); ys = vd.gety(); } 
                  else if (me == ent2) // Entity2
                  { xe = vd.getx(); ye = vd.gety(); }  
                }
                LineData sline = 
                  new LineData(xs,ys,xe,ye,linecount,SOLID);
                sline.setModelElement(ast); 
                visuals.add(sline); 
              }
            }
          }
        } 
      }
    }  */
    repaint(); 
  } 

  private Vector parseCSVRow(String line)
  { if (line == null) { return null; } 

    Vector line1vals = new Vector();
    StringTokenizer st1 =
      new StringTokenizer(line, ";");  // delimiter is semicolon

    while (st1.hasMoreTokens())
    { String str = st1.nextToken(); 
      if (str != null) 
      { line1vals.add(str.trim()); }
    } 
    return line1vals; 
  } 

  private PreEntity parseEntity(BufferedReader br)
  { String line1;
    String line2;
    String line3; 
    Vector line1vals = new Vector();
    Vector line2vals = new Vector();
    Vector line3vals = new Vector(); 
    String fname = null; 
    String ecard = null; 
    Vector stereotypes = new Vector(); 

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading entity details failed");
      return null; 
    }
    StringTokenizer st1 =
      new StringTokenizer(line1);

    try { line2 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading entity superclass failed");
      return null; 
    }

    try { line3 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading attribute details failed");
      return null;
    }

    StringTokenizer st2 =
      new StringTokenizer(line2);

    while (st1.hasMoreTokens())
    { line1vals.add(st1.nextToken()); }

    while (st2.hasMoreTokens())
    { line2vals.add(st2.nextToken()); }

    if (line2vals.size() < 2)
    { System.err.println("No superclass/cardinality for entity"); } 
    else 
    { fname = (String) line2vals.get(0); 
      ecard = (String) line2vals.get(1); 
      for (int p = 2; p < line2vals.size(); p++) 
      { stereotypes.add(line2vals.get(p)); } 
    } // superclass name + cardinality, or "null"s

    if (line1vals.size() != 3)
    { System.err.println("Incorrect data for entity -- no component " + 
                         "name/x,y-coords");
      return null; 
    }

    String nme = (String) line1vals.get(0);
    String xs = (String) line1vals.get(1);
    String ys = (String) line1vals.get(2);
    int xx;
    int yy;

    try
    { xx = Integer.parseInt(xs); }
    catch (NumberFormatException nfe)
    { System.err.println("X coordinate not a number! " + xs);
      return null;
    }
    try
    { yy = Integer.parseInt(ys); }
    catch (NumberFormatException nfe)
    { System.err.println("Y coordinate not a number! " + ys);
      return null;
    }
    // System.out.println("Read data: " + nme + " " + xx + " " + yy + 
    //                    " " + fname); 

    StringTokenizer st3 = new StringTokenizer(line3);
    while (st3.hasMoreTokens())
    { String s = st3.nextToken();
      line3vals.add(s);
    }

    int count = line3vals.size();
    int n = count/6;
    if (count != n*6)
    { System.out.println("Error: wrong number of attribute data");
      return null;
    }

    int[] attmode = new int[n];
    Vector attnmes = new Vector();
    Vector atttypes = new Vector();
    Vector attfrozs = new Vector();
    Vector attuniqs = new Vector(); 
    Vector attstatics = new Vector(); // and initial values

    for (int i = 0; i < n; i++)
    { String an = (String) line3vals.get(6*i);
      String at = (String) line3vals.get(6*i+1);
      String am = (String) line3vals.get(6*i+2);
      String afroz = (String) line3vals.get(6*i + 3); 
      String auniq = (String) line3vals.get(6*i + 4); 
      String astatic = (String) line3vals.get(6*i + 5); 
      // System.out.println("Retrived attribute: " + an + " " + at + " " + am); 
      attnmes.add(an);
      atttypes.add(at);
      attfrozs.add(afroz); 
      attuniqs.add(auniq);
      attstatics.add(astatic); 

      try
      { attmode[i] = Integer.parseInt(am); }
      catch (Exception e) { return null; }
    }
    Entity ent = reconstructEntity(nme,xx,yy,fname,ecard,stereotypes); 
    // System.out.println("Reconstructed entity " + ent); 
    return new PreEntity(ent,attnmes,atttypes,attmode,
                         attfrozs,attuniqs,attstatics);
  } 

  private PreOp parseOperation(BufferedReader br)
  { String line1; // name
    String line2; // entity name, use case name
    String line3; // result type -- void means update op.
    String line4; // parameter list
    String line5; // stereotypes
    String spre; 
    String spost; 

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading operation name failed");
      return null; 
    }

    try { line2 = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operation entity failed"); 
      return null; 
    }

    try { line3 = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operation type failed"); 
      return null; 
    }  // will be "void" if no return

    try { line4 = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operation parameters failed"); 
      return null; 
    }

    try { line5 = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operation stereotypes failed"); 
      return null; 
    }

    try { spre = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operation precondition failed"); 
      return null; 
    }

    try { spost = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operation postcondition failed"); 
      return null; 
    }
    
    // Vector pns = new Vector(); 
    // Vector pts = new Vector(); 
    return new PreOp(line1,line2,line4,line3,spre,spost,line5); 
  } 

  private PreOperator parseOperator(BufferedReader br)
  { String line1; // name and type
    String line2; // java
    String line3; // C# 

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading operator name failed");
      return null; 
    }

    try { line2 = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operator java failed"); 
      return null; 
    }

    try { line3 = br.readLine(); } 
    catch (IOException e)
    { System.err.println("Reading operator C# failed"); 
      return null; 
    }  // will be "void" if no return

    /* and C++ */ 

    return new PreOperator(line1,line2,line3); 
  } 

  private PreAssociation parseAssociation(BufferedReader br) 
  { String line1; // coordinates, multiplicities, roles
    String line2; // stereotypes
    String line3; // waypoints

    Vector line1vals = new Vector();
    Vector line2vals = new Vector();
    Vector wpoints = new Vector(); 
   
    int xs, ys, xe, ye, c1, c2; 

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading association details failed");
      return null;
    }
    StringTokenizer st1 =
      new StringTokenizer(line1);

    try { line2 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading association details failed");
      return null;
    }
    StringTokenizer st2 =
      new StringTokenizer(line2);

    while (st1.hasMoreTokens())
    { line1vals.add(st1.nextToken()); }

    while (st2.hasMoreTokens())
    { line2vals.add(st2.nextToken()); }


    if (line1vals.size() < 9)
    { System.err.println("Incorrect data for association -- no cards, roles " +
                         "x,y-coords");
      return null;
    }

    String e1n = (String) line1vals.get(0);
    String e2n = (String) line1vals.get(1); 
    String c1s = (String) line1vals.get(2);
    String xss = (String) line1vals.get(3);
    String yss = (String) line1vals.get(4);
    String xes = (String) line1vals.get(5);
    String yes = (String) line1vals.get(6);
    String c2s = (String) line1vals.get(7);
    String role2 = (String) line1vals.get(8);
    String role1 = null; 
    if (line1vals.size() > 9)
    { role1 = (String) line1vals.get(9); }  // will be "null" if not specified

    Vector stereotypes = new Vector(); // includes readOnly, addOnly, ordered, qualified 
    for (int p = 0; p < line2vals.size(); p++)
    { stereotypes.add(line2vals.get(p)); }

    try
    { xs = Integer.parseInt(xss); }
    catch (NumberFormatException nfe)
    { System.err.println("X coordinate not a number! " + xss);
      return null;
    }
    try
    { ys = Integer.parseInt(yss); }
    catch (NumberFormatException nfe)
    { System.err.println("Y coordinate not a number! " + yss);
      return null;
    }
    try
    { xe = Integer.parseInt(xes); }
    catch (NumberFormatException nfe)
    { System.err.println("X coordinate not a number! " + xes);
      return null;
    }
    try
    { ye = Integer.parseInt(yes); }
    catch (NumberFormatException nfe)
    { System.err.println("Y coordinate not a number! " + yes);
      return null;
    }
    try
    { c1 = Integer.parseInt(c1s); }
    catch (NumberFormatException nfe)
    { System.err.println("Card 1 not a number! " + c1s);
      return null;
    }
    try
    { c2 = Integer.parseInt(c2s); }
    catch (NumberFormatException nfe)
    { System.err.println("Card 2 not a number! " + c2s);
      return null;
    }

    if (line1vals.size() % 2 == 0)
    { for (int i = 10; i < line1vals.size(); i = i + 2)
      { int wx, wy; 
        try { wx = Integer.parseInt((String) line1vals.get(i)); 
              wy = Integer.parseInt((String) line1vals.get(i+1)); 
              LinePoint p = new LinePoint(wx,wy); 
              wpoints.add(p); 
            } 
        catch (Exception e2) { System.err.println("Not valid waypoints"); }
      } 
    } 

    // System.out.println("Read data: " + e1n + " " + e2n + " " + 
    //                    c1s + " " + xss + " " + yss + " " + 
    //                    xes + " " + yes + " " + c2s + " " + role2 + " " + role1 +  
    //                    " " + stereotypes);
    // System.out.println("Waypoints: " + wpoints); 
    return new PreAssociation(e1n,e2n,c1,c2,xs,ys,xe,ye,role2,role1,stereotypes,wpoints); 
  }

  public PreGeneralisation parseGeneralisation(BufferedReader br)
  { String line1;
    Vector line1vals = new Vector();
    String line2;
    Vector line2vals = new Vector();
    // String line3;
    // Vector line3vals = new Vector();
    Vector wpoints = new Vector(); 

    int xs, ys, xe, ye;

    try { line1 = br.readLine(); }
    catch (Exception e)
    { System.err.println("Error reading generalisation");
      return null;
    }

    StringTokenizer st = new StringTokenizer(line1);
    while (st.hasMoreTokens())
    { line1vals.add(st.nextToken()); }

    if (line1vals.size() != 6)
    { System.err.println("Incorrect data for generalisation");
      return null;
    }
    String e1name = (String) line1vals.get(0);  // superclass
    String e2name = (String) line1vals.get(1);  // subclass
    String xss = (String) line1vals.get(2);
    String yss = (String) line1vals.get(3);
    String xes = (String) line1vals.get(4);
    String yes = (String) line1vals.get(5);

    try
    { xs = Integer.parseInt(xss); }
    catch (Exception e)
    { System.err.println("Not an int: " + xss);
      return null;
    }
    try
    { ys = Integer.parseInt(yss); }
    catch (Exception e)
    { System.err.println("Not an int: " + yss);
      return null;
    }
    try
    { xe = Integer.parseInt(xes); }
    catch (Exception e)
    { System.err.println("Not an int: " + xes);
      return null;
    }
    try
    { ye = Integer.parseInt(yes); }
    catch (Exception e)
    { System.err.println("Not an int: " + yes);
      return null;
    }
    // System.out.println("Read data: " + e1name + " " +
    //                    e2name + " " + xss + " " +
    //                    yss + " " + xes + " " + yes);

    try { line2 = br.readLine(); }
    catch (Exception e)
    { System.err.println("Error reading generalisation line 2");
      return null;
    }

    StringTokenizer st2 = new StringTokenizer(line2);
    while (st2.hasMoreTokens())
    { line2vals.add(st2.nextToken()); }

    // System.out.println("Read line 2: " + line2vals); 

    if (line2vals.size() % 2 == 0)
    { for (int i = 0; i < line2vals.size(); i = i + 2)
      { int wx, wy; 
        try { wx = Integer.parseInt((String) line2vals.get(i)); 
              wy = Integer.parseInt((String) line2vals.get(i+1)); 
              LinePoint p = new LinePoint(wx,wy); 
              wpoints.add(p); 
            } 
        catch (Exception e2) { System.err.println("Not valid waypoints"); }
      } 
    } 

    /* try { line3 = br.readLine(); }
    catch (Exception e)
    { System.err.println("Error reading generalisation line 3");
      return null;
    }

    StringTokenizer st3 = new StringTokenizer(line3);
    while (st3.hasMoreTokens())
    { line3vals.add(st3.nextToken()); }

    System.out.println("Read line 3: " + line3vals); */

    return new PreGeneralisation(e1name,e2name,xs,ys,
                                  xe,ye,wpoints);
  }

  private Type parseType(BufferedReader br) 
  { String line1; // has the name + xx + yy
    String line2; // values, if any
    Vector line1vals = new Vector(); 

    try
    { line1 = br.readLine(); }
    catch (Exception e)
    { System.out.println("Failed to read type name");
      return null;
    }

    try
    { line2 = br.readLine(); }
    catch (Exception e)
    { System.out.println("Failed to read type values");
      return null;
    }

    StringTokenizer st = new StringTokenizer(line1); 
    while (st.hasMoreTokens())
    { line1vals.add(st.nextToken()); }

    Vector vals = new Vector();
    int xx, yy; 

    if (line2.equals("") || line2.equals("null"))  // or "null"
    { vals = null; }
    else 
    { StringTokenizer st2 = new StringTokenizer(line2);
      while (st2.hasMoreTokens())
      { String s = st2.nextToken();
        vals.add(s);
      }
    } 

    if (line1vals.size() != 3) 
    { System.out.println("Failed to read type name");
      return null;
    }

    String nme = (String) line1vals.get(0); 
    String xs = (String) line1vals.get(1);
    String ys = (String) line1vals.get(2);
    try
    { xx = Integer.parseInt(xs); }
    catch (NumberFormatException nfe)
    { System.err.println("X coordinate not a number! " + xs);
      return null;
    }
    try
    { yy = Integer.parseInt(ys); }
    catch (NumberFormatException nfe)
    { System.err.println("Y coordinate not a number! " + ys);
      return null;
    }

    Type t = new Type(nme,vals); 
    types.add(t); 
    if (vals != null) 
    { RectData rd = new RectData(xx,yy,getForeground(),
                                 componentMode,
                                 rectcount);
      rectcount++;
      rd.setLabel(nme);
      rd.setModelElement(t); 
      visuals.add(rd); 
    } 
    // System.out.println("retrieved type " + t); 
    return t; 
  } 

  private PreUseCase parseUseCase(BufferedReader br)
  { String line1;
    Vector line1vals = new Vector();
    String nme = null; 
    String ename = null; 
    Vector stereotypes = new Vector(); 
    String role = null; 

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading EIS usecase details failed");
      return null; 
    }
    StringTokenizer st1 =
      new StringTokenizer(line1);

    while (st1.hasMoreTokens())
    { line1vals.add(st1.nextToken()); }

    if (line1vals.size() < 3)
    { System.err.println("No name, entity and op for use case"); 
      return null; 
    } 

    nme = (String) line1vals.get(0);
    ename = (String) line1vals.get(1);
    String op = (String) line1vals.get(2);
    for (int p = 2; p < line1vals.size(); p++) 
    { stereotypes.add(line1vals.get(p)); } 
    // System.out.println("Read data: " + nme + " " + ename + " " + op + 
    //                    " " + stereotypes); 
    // System.out.println("Reconstructed use case");
    if (line1vals.size() > 3)
    { role = (String) line1vals.get(3); }  
    return new PreUseCase(op,ename,role); 
  } 

  private UseCase parseGeneralUseCase(BufferedReader br)
  { String line1, line2, line3, line4, line5;
    Vector line1vals = new Vector();  // name par1 type1 par2 type2 ...
    Vector line2vals = new Vector();  // extension1 extension2 ...
    Vector line3vals = new Vector();  // included1 included2 ...
    Vector line4vals = new Vector();  // att1 type1 ...

    String nme = null; 
    Vector params = new Vector();  // assumed to be of basic types
    String desc = null; 
    Statement code = null; 

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading general usecase details failed");
      return null; 
    }
    StringTokenizer st1 =
      new StringTokenizer(line1);

    while (st1.hasMoreTokens())
    { line1vals.add(st1.nextToken()); }

    nme = (String) line1vals.get(0);

    ModelElement me = ModelElement.lookupByName(nme, useCases); 
    if (me != null) 
    { System.err.println("ERROR: Duplicated declaration of " + nme); 
      return null; 
    } 

    UseCase res = new UseCase(nme,null);
    OvalData od = new OvalData(10,80*useCases.size(),getForeground(),useCases.size()); 
    od.setName(nme); 
    od.setModelElement(res); 
    visuals.add(od);
    entities.add(res.classifier); 

    for (int i = 1; i < line1vals.size(); i = i+2)
    { String par = (String) line1vals.get(i);
      String typ = (String) line1vals.get(i+1);  // can be general types Set(E)
      Type tt = null; 
      Type elemType = null; 

      if (typ != null) 
      { tt = Type.getTypeFor(typ, types, entities); 
        if (tt != null) 
        { elemType = tt.getElementType(); }    
      }      

      if ("result".equals(par))
      { res.setResultType(tt); 
        if (elemType != null) 
        { res.setElementType(elemType); } 
      } 
      else       
      { Attribute ucatt = new Attribute(par,tt,ModelElement.INTERNAL);
        ucatt.setElementType(elemType); 
        params.add(ucatt); 
      } 
    } 

    // res.setDescription(desc); 
    res.setParameters(params); 

    try { line2 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading usecase extends failed");
      return res; 
    }
    StringTokenizer st2 =
      new StringTokenizer(line2);

    while (st2.hasMoreTokens())
    { String extend = st2.nextToken(); 
      line2vals.add(extend); 
      UseCase extensionuc = 
          (UseCase) ModelElement.lookupByName(extend,useCases); 
      if (extensionuc == null) 
      { System.err.println("Extension use case: " + extend + " does not exist"); }
      else 
      { Extend ext = new Extend(res,extensionuc); 
        res.addExtension(ext); 
        drawDependency(extensionuc, res, "<<extend>>"); 
      }
    } 
    // System.out.println(line2vals);

    try { line3 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading usecase includes failed");
      return res; 
    }
    StringTokenizer st3 =
      new StringTokenizer(line3);

    while (st3.hasMoreTokens())
    { String include = st3.nextToken(); 
      line3vals.add(include); 
      UseCase ucinc = (UseCase) ModelElement.lookupByName(include,useCases); 

      if (ucinc != null) 
      { Include ee = new Include(res,ucinc); 
        res.addInclude(ee); 
        drawDependency(res, ucinc, "<<include>>"); 
      } 
      else 
      { System.err.println("Included use case: " + include + " does not exist"); }
    }
    // System.out.println(line3vals);

    try { line4 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading usecase attributes failed");
      return res; 
    }
    StringTokenizer st4 =
      new StringTokenizer(line4);

    while (st4.hasMoreTokens())
    { line4vals.add(st4.nextToken()); }  

    for (int i = 0; i+1 < line4vals.size(); i = i+2)
    { String par = (String) line4vals.get(i);
      String typ = (String) line4vals.get(i+1);  // can be general types Set(E)
      Type elemType = null; 
      Type tt = null; 

      if (typ != null) 
      { tt = Type.getTypeFor(typ,types,entities); 
        if (tt != null) 
        { elemType = tt.getElementType(); } 
      }      

      Attribute ucatt = new Attribute(par,tt,ModelElement.INTERNAL);
      ucatt.setElementType(elemType); 
      res.addAttribute(ucatt);  
    } 

    try { line5 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading usecase incremental failed");
      return null; 
    }
    // System.out.println("INCREAMENT" + line5); 
    if (line5 != null && "true".equals(line5.trim()))
    { res.setIncremental(true); } 

    return res;  
  } 

  private PreConstraint parseConstraint(BufferedReader br)
  { String line1;  // cond0
    String line2;  // cond
    String line3;  // succ
    String line4;  // associations, class name, 
    String line5 = "";  // use case name, if any
    Vector line4vals = new Vector();
    Expression cond0 = null; 
    Expression cond = null; 
    Expression succ = null; 
    Expression orderedBy = null; 

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading constraint cond0 failed");
      return null;
    }
    try { line2 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading constraint cond failed");
      return null;
    }
    try { line3 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading constraint succ failed");
      return null;
    }
    try { line4 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading constraint associations failed");
      return null;
    }
    try { line5 = br.readLine(); }
    catch (IOException e)
    { System.err.println("Reading constraint ordering failed");
      // return null;
    }

    StringTokenizer st =
      new StringTokenizer(line4);

    while (st.hasMoreTokens())
    { line4vals.add(st.nextToken()); }

    Compiler2 comp2 = new Compiler2();
    
    if (line1.equals("null"))
    { cond0 = null; } 
    else 
    { comp2.nospacelexicalanalysis(line1); 
      cond0 = comp2.parse(); 
    } 

    Compiler2 comp3 = new Compiler2();

    if (line2.equals("null"))
    { cond = null; }   // or new BasicExpression("true") ??? 
    else
    { comp3.nospacelexicalanalysis(line2);
      cond = comp3.parse();
    }

    Compiler2 comp4 = new Compiler2();

    if (line3.equals("null"))
    { succ = null; 
      System.err.println("ERROR: null succedent for constraint"); 
      return null; 
    }
    else
    { comp4.nospacelexicalanalysis(line3);
      succ = comp4.parse();
    }

    if (line5 == null || "".equals(line5) || "false".equals(line5)) { } 
    else 
    { comp4 = new Compiler2(); 
      comp4.nospacelexicalanalysis(line5); 
      orderedBy = comp4.parse(); 
    } 

    PreConstraint cons = new PreConstraint(cond0,cond,succ,line4vals,orderedBy);
    // System.out.println("Retrived constraint: " + cons); 
    return cons; 
  } 
  // and line5 = use case

  private PreBehaviour parseBehaviour(BufferedReader br)
  { String line1;
    String line2;
    Vector line1vals = new Vector();

    try { line1 = br.readLine(); }
    catch (IOException e)
    { System.err.println(">>>> Reading behaviour details failed");
      return null; 
    }
    StringTokenizer st1 =
      new StringTokenizer(line1);

    try { line2 = br.readLine(); }
    catch (IOException e)
    { System.err.println(">>>> Reading activity code failed");
      return null; 
    }

    while (st1.hasMoreTokens())
    { line1vals.add(st1.nextToken()); }

    if (line1vals.size() < 1)
    { System.err.println("ERROR: Incorrect data for activity -- no entity/use case " + 
                         "name, operation name");
      return null; 
    }

    String nme = (String) line1vals.get(0); 
    String op = ""; 
    if (line1vals.size() > 1)
    { op = (String) line1vals.get(1); } 

    Compiler2 comp = new Compiler2();
    Statement cde; 

    if (line2.equals("null") || line2.length() == 0)
    { cde = null; } 
    else 
    { comp.nospacelexicalanalysis(line2); 
      cde = comp.parseStatement(entities,types); 
    } 

    if (cde == null) 
    { System.err.println("ERROR: invalid syntax for activity " + line2); } 

    // System.out.println("Read activity data: " + nme + " " + op + " " + cde); 

    return new PreBehaviour(nme,op,cde);
  } 


  private InheritanceFamily findFamily(Entity e)
  { for (int i = 0; i < families.size(); i++)
    { InheritanceFamily f =
        (InheritanceFamily) families.get(i);
      if (f.hasMember(e))
      { return f; }
    }
    return null;
  }

  private boolean formFamilies(Generalisation g)
  { Entity desc = g.getDescendent();
    Entity ansc = g.getAncestor();
    InheritanceFamily f1 = findFamily(desc);
    InheritanceFamily f2 = findFamily(ansc);

    if (f1 == null && f2 == null)
    { InheritanceFamily ff = new InheritanceFamily();
      ff.add(desc,ansc);
      families.add(ff);
    }
    else if (f1 == f2)
    { System.out.println("Error: duplicate " +
            "inheritance " + g);
      f1.invalidate();
      return false; 
    }
    else if (f1 == null)
    { f2.addMember(desc); } 
    else if (f2 == null)
    { if (f1.hasMaximal(desc))
      { f1.replaceMaximal(desc,ansc); }
      else
      { System.out.println("Error: multiple " +
                           "inheritance for " + desc);
        f1.addMaximal(ansc);
        f1.invalidate();
        return false; 
      }
    }
    else // f1, f2 non-null and distinct
    { if (f1.hasMaximal(desc))
      { f1.pureUnion(f2,desc);
        families.remove(f2);
      }
      else
      { System.out.println("Error: multiple inheritance"
                           + " for " + desc);
        f1.impureUnion(f2);
        families.remove(f2);
        return false; 
      }
    }
    System.out.println("New inheritance families: " + families); 
    return true; 
  }

  private void removeFamily(Generalisation g)
  { if (g == null) { return; } 

    Entity desc = g.getDescendent();
    Vector subs1 = desc.getSubclasses();
    Entity ansc = g.getAncestor();
    Vector subs2 = ansc.getSubclasses();
    InheritanceFamily f = findFamily(ansc);

    if (f == null) { return; } 

    if (f.hasMaximal(ansc))
    { if (subs2.size() == 1) 
      { f.remove(ansc); 
        if (subs1.size() > 0)
        { f.replaceMaximal(ansc,desc); } 
        else 
        { f.remove(desc);
          families.remove(f);
        } 
      } 
      else if (subs2.size() > 1)
      { if (subs1.size() == 0)
        { f.remove(desc); }
        else // separate family for it
        { InheritanceFamily f2 = f.splitFamily(desc);
          families.add(f2);
        }
      }  // split ansc family from desc
      else
      { f.remove(ansc); // from maximals and members
        if (subs1.size() > 0)
        { f.addMaximal(desc); }
        else
        { f.remove(desc);
          if (f.size() == 0)
          { families.remove(f); }
        }
      }
    }
    else
    { if (subs1.size() == 0)
      { f.remove(desc); }
      else // separate family for it
      { InheritanceFamily f2 = f.splitFamily(desc);
        families.add(f2);
      }
    }
    System.out.println("New inheritance families: " + families); 
  }

  public boolean consistencyCheck()  // checks model properties, and invariants
  { boolean res = true; 
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i); 
      res = res && e.checkAttributeRedefinitions(); 
      if (e.selfImplementing())
      { System.err.println("Error: cycle of interfaces: " + e);
        res = false; 
      } 
    } 
    return res; 
  }
   
  public boolean checkCompleteness()
  { boolean complete = true;
    Vector allsens = new Vector();
    Vector allactint = new Vector();
    Vector conslhs = new Vector();
    Vector consrhs = new Vector();

    for (int i = 0; i < entities.size(); i++)
    { Entity ent = (Entity) entities.get(i);
      Vector sensf = ent.allSenFeatures();
      allsens.addAll(sensf);
      Vector aif = ent.allActIntFeatures();
      allactint.addAll(aif);
      conslhs.addAll(ent.allLhsFeatures()); 
      consrhs.addAll(ent.allRhsFeatures());  // from local invs
      System.out.println("Data flows for local invariants of entity " + ent + ": " + conslhs + " to: " + consrhs); 
    }

    for (int j = 0; j < constraints.size(); j++)
    { Constraint con = (Constraint) constraints.get(j);
      conslhs.addAll(con.allLhsFeatures());
      consrhs.addAll(con.allRhsFeatures());
      DataDependency dd = con.getDataFlows();
      System.out.println("Data flows for " + con + " are: " + dd); 
    }

    for (int k = 0; k < allsens.size(); k++)
    { String sf = (String) allsens.get(k);
      if (conslhs.contains(sf)) { }
      else
      { System.out.println("WARNING: No use of sensor: " + sf);
        System.out.println("in any constraint lhs -- " +
                           "constraints may be incomplete.\n");
        
        complete = false;
      }
    }

    for (int k = 0; k < allactint.size(); k++)
    { String af = (String) allactint.get(k);
      if (consrhs.contains(af)) { }
      else
      { System.out.println("WARNING: No use of actuator: " + af);
        System.out.println("in any constraint rhs -- " +
                           "constraints may be incomplete.\n");
        complete = false;
      }
    }
    boolean comp2 = checkValueCompleteness(); 
    return complete && comp2;
  }

  private boolean checkValueCompleteness()
  { boolean complete = true;
    Vector allsens = new Vector();
    Vector allactint = new Vector();
    Vector conslhs = new Vector();
    Vector consrhs = new Vector();

    for (int i = 0; i < entities.size(); i++)
    { Entity ent = (Entity) entities.get(i);
      Vector sensf = ent.allSenValues();
      allsens.addAll(sensf);
      Vector aif = ent.allActIntValues();
      allactint.addAll(aif);
      conslhs.addAll(ent.allLhsValues()); 
      consrhs.addAll(ent.allRhsValues());  // from local invs
    }

    for (int j = 0; j < constraints.size(); j++)
    { Constraint con = (Constraint) constraints.get(j);
      conslhs.addAll(con.allLhsValues());
      consrhs.addAll(con.allRhsValues()); 
    }

    for (int k = 0; k < allsens.size(); k++)
    { String sf = (String) allsens.get(k);
      if (conslhs.contains(sf)) { }
      else
      { System.out.println("WARNING: No use of sensor value: " + sf);
        System.out.println("in any constraint lhs -- " +
                           "constraints may be incomplete.\n");
        complete = false;
      }
    }

    for (int k = 0; k < allactint.size(); k++)
    { String af = (String) allactint.get(k);
      if (consrhs.contains(af)) { }
      else
      { System.out.println("WARNING: No use of actuator value: " + af);
        System.out.println("in any constraint rhs -- " +
                           "constraints may be incomplete.\n");
        complete = false;
      }
    }
    return complete;
  }  // Need more precise analysis, specific to individual features. 

/*  public Vector derivedInvariants(Invariant inv)
  { Vector res = new Vector();
    Expression ante = inv.antecedent();
    Expression succ = inv.succedent();
    if (ante == null)
    { res.add(inv); }
    else 
    { Vector bes = ante.allBinarySubexpressions();
      if (bes.size() == 0)
      { res.add(inv); 
        return res; 
      }
      for (int i = 0; i < bes.size(); i++)
      { Expression e = (Expression) bes.get(i);
        Expression rem = ante.removeExpression(e);
        Expression newante =
          Expression.simplify("&",e,rem,new Vector());
        Invariant newinv = 
          new SafetyInvariant(newante,succ);
        res.add(newinv);
      }
    } // if bes is empty?
    return res;
  } */ 

  public boolean diagramCheck()
  { boolean ok = true; 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      Entity e1 = ast.getEntity1(); 
      Entity e2 = ast.getEntity2(); 
      if (entities.contains(e1)) { } 
      else 
      { ok = false; 
        System.err.println("Entity " + e1 + " not present: invalid diagram"); 
      } 
      if (entities.contains(e2)) { } 
      else 
      { ok = false; 
        System.err.println("Entity " + e2 + " not present: invalid diagram"); 
      } 
    }
    return ok; 
  } // and for entities, and generalisations


  public String generateXml()
  { String res = "<xmi:XMI version=\"2.0\"\n" +
    "xmlns:UML=\"http://schema.omg.org/spec/UML/1.4\"\n"
    +
   "xmlns:xmi=\"http://schema.omg.org/spec/XMI/2.0\">\n";
    for (int i = 0; i < entities.size(); i++)
    { Entity e = (Entity) entities.get(i);
      res = res + e.toXml();
    }
    for (int j = 0; j < types.size(); j++)
    { Type t = (Type) types.get(j);
      res = res + t.toXml();
    }
    return res + "</xmi:XMI>";
  }  // is this sufficient? Use cases? 

  public String generateDbi(Vector operations)
  { String res = "import java.sql.*; \n\n" +
      "public class Dbi\n" +
      "{ private Connection connection;\n" +
      "  private static String defaultDriver = \"\"; \n" + 
      "  private static String defaultDb = \"\"; \n" + 
      getPreparedStatDecs(operations);
    res = res +
      "  public Dbi() { this(defaultDriver,defaultDb); } \n\n" + 
      "  public Dbi(String driver, String db)\n" +
      "  { try \n" +
      "    { Class.forName(driver); \n" +
      "      connection = " +
      "DriverManager.getConnection(db);\n";
 
    for (int i = 0; i < operations.size(); i++)
    { OperationDescription od = 
          (OperationDescription) operations.get(i);
      SQLStatement odstat = od.getSQL0();
      System.out.println(odstat);
      String odname = od.getODName();  
      if (odstat != null) 
      { res = res + "      " + odname +
          "Statement = connection.prepareStatement(" +
          odstat.preparedStatement() + ");\n";
      } 
    }
    res = res + "    } catch (Exception e) { }\n" +
                "  }\n\n";
       
    for (int i = 0; i < operations.size(); i++)
    { OperationDescription od = 
          (OperationDescription) operations.get(i);
      String odname = od.getODName();  
      String pars = od.getDbiParameterDec();
      String code = od.getDbiOpCode();
      String resultType = "void"; 
      String odaction = od.getAction();
      if (odaction.equals("get") || odaction.equals("list") || odaction.equals("searchBy") || 
          odaction.equals("check"))
      { resultType = "ResultSet"; } 
      res = res + 
        "  public synchronized " + resultType + " " + odname + "(" +
        pars + ")\n  " + code + "\n";
    }
    return res + "  public synchronized void logoff() \n" + 
                 "  { try { connection.close(); } \n" + 
                 "    catch (Exception e) { e.printStackTrace(); }\n" + 
                 "  }\n}\n";
  }

  public String generateJspDbi(Vector operations)
  { String res = "package beans;\n\n" + 
      "import java.sql.*; \n\n" +
      "public class Dbi\n" +
      "{ private Connection connection;\n" +
      "  private static String defaultDriver = \"\"; \n" + 
      "  private static String defaultDb = \"\"; \n" + 
      getPreparedStatDecs(operations);
    res = res +
      "  public Dbi() { this(defaultDriver,defaultDb); } \n\n" + 
      "  public Dbi(String driver, String db)\n" +
      "  { try \n" +
      "    { Class.forName(driver); \n" +
      "      connection = " +
      "DriverManager.getConnection(db);\n";
 
    for (int i = 0; i < operations.size(); i++)
    { OperationDescription od = 
          (OperationDescription) operations.get(i);
      SQLStatement odstat = od.getSQL0();
      System.out.println(odstat);
      String odname = od.getODName();  
      if (odstat != null) 
      { res = res + "      " + odname +
          "Statement = connection.prepareStatement(" +
          odstat.preparedStatement() + ");\n";
      } 
    }
    res = res + "    } catch (Exception e) { }\n" +
                "  }\n\n";
       
    for (int i = 0; i < operations.size(); i++)
    { OperationDescription od = 
          (OperationDescription) operations.get(i);
      String odname = od.getODName();  
      String pars = od.getDbiParameterDec();
      String code = od.getDbiOpCode();
      String resultType = "void"; 
      String odaction = od.getAction();
      if (odaction.equals("get") || odaction.equals("list") || odaction.equals("searchBy") || 
          odaction.equals("check"))
      { resultType = "ResultSet"; } 
      res = res + 
        "  public synchronized " + resultType + " " + odname + "(" +
        pars + ")\n  " + code + "\n";
      String mops = od.getMaintainOps(); 
      res = res + mops; 
    }
    return res + "  public synchronized void logoff() \n" + 
                 "  { try { connection.close(); } \n" + 
                 "    catch (Exception e) { e.printStackTrace(); }\n" + 
                 "  }\n}\n";
  }

  private String getPreparedStatDecs(Vector ops)
  { String res = "";
    for (int i = 0; i < ops.size(); i++)
    { OperationDescription od = 
          (OperationDescription) ops.get(i);
      String odname = od.getODName();
      res = res +
            "  private PreparedStatement " + odname +
            "Statement;\n"; 
    }
    return res;
  }

  private String generateDbiPool()
  { String res = "package beans;\n\n" + 
      "import java.util.Vector;\n\n" + 
      "public class DbiPool\n" + 
      "{ private Vector free = new Vector(); \n" + 
      "  private Vector used = new Vector(); \n\n" +
      "  public DbiPool()\n" + 
      "  { this(10); }\n\n" +  
      "  public DbiPool(int n)\n" + 
      "  { for (int i = 0; i < n; i++)\n" + 
      "    { free.add(new Dbi()); }\n" +
      "  }\n\n" +  
      "  public Dbi getConn()\n" + 
      "  { if (free.size() > 0)\n" + 
      "    { Dbi db = (Dbi) free.get(0); \n" + 
      "      free.remove(0); \n" + 
      "      used.add(db);\n" + 
      "      return db; \n" + 
      "    }\n" + 
      "    else \n" + 
      "    { Dbi db = new Dbi();\n" + 
      "      used.add(db);\n" + 
      "      return db;\n" + 
      "    }\n" +
      "  }\n\n" + 
      "  public void releaseConn(Dbi db)\n" + 
      "  { free.add(db); \n" + 
      "    used.remove(db); \n" + 
      "  }\n" + 
      "}\n";
    return res; 
  }

   public String generateBasePage(String title)
   { String res = "public class BasePage\n" +
       "{ protected HtmlPage page = new HtmlPage();\n" + 
       "  protected HtmlHead head = \n" +
       "    new HtmlHead(\"" + title + "\");\n" + 
       " protected HtmlBody body = new HtmlBody();\n\n" +
       "  public BasePage()\n" +
       "  { page.setHead(head);\n" +
       "    page.setBody(body);\n" +
       "  } \n\n" +
       "  public String toString() \n" +
       "  { return page.getHtml(); } \n" +
       "} \n";
    return res;
  }

  public String generateCommandPage(java.util.List ops)
  { String res = "public class CommandPage extends BasePage\n" +
      "{ private HtmlForm form = new HtmlForm();\n"; 
    for (int i = 0; i < ops.size(); i++)
    { OperationDescription od = 
        (OperationDescription) ops.get(i);
      String nme = od.getODName();
      res = res +
        "  private HtmlInput " + nme +
        "button = new HtmlInput();\n";
    }
    res = res + "\n  public CommandPage()\n" +
      "  { super();\n" +
      "    form.setAttribute(\"method\",\"POST\");\n" +             
      "        form.setAttribute(\"action\",\n" + 
      "               \"http://localhost:8080/servlet/CommandServlet\");\n";
    for (int i = 0; i < ops.size(); i++)
    { OperationDescription od = 
        (OperationDescription) ops.get(i);
      String odnme = od.getODName();
      res = res + 
        "    " + odnme + 
        "button.setAttribute(\"value\",\"" + odnme +
        "\");\n" +
        "    " + odnme + 
        "button.setAttribute(\"name\",\"" + odnme +
        "\");\n" +
        "    " + odnme + 
        "button.setAttribute(\"type\",\"submit\");\n" +
        "    form.add(" + odnme + "button);\n";
    }
    res = res + "    body.add(form);\n" +
                "  }\n}\n";
    return res;
  }

  public String generateCommandHtml(java.util.List ops)
  { String res = "<html><head><title>Commands</title></head>\n\r" + 
                 "<body><h1>Commands"; 
    if (systemName != null) 
    { res = res + " " + systemName; } 
    res = res + "</h1>\n\r";

    for (int i = 0; i < ops.size(); i++)
    { OperationDescription od = 
        (OperationDescription) ops.get(i);
      String odnme = od.getODName();
      res = res + 
        "<p><a href=\"" + odnme + ".html\">" + odnme + "</a></p>\n\r";  
    }
    return res + "</html>\n\r";
  }

  public String generateCommandServlet(Vector ops)
  { String res = "import java.io.*;\n" +
          "import java.util.*;\n" +
          "import javax.servlet.http.*;\n" +
          "import javax.servlet.*;\n";

    res = res + 
      "public class CommandServlet extends HttpServlet\n"; 
    res = res + "{ " +
          "  public CommandServlet() {}\n\n";

    res = res + "  public void init(ServletConfig cfg)\n" +
          "  throws ServletException\n" +
          "  { super.init(cfg); }\n\n"; 
    res = res + 
      "  public void doGet(HttpServletRequest req,\n" +
      "              HttpServletResponse res)\n" + 
      "  throws ServletException, IOException\n" +
      "  { res.setContentType(\"text/html\");\n" +
      "    PrintWriter pw = res.getWriter();\n";

    for (int i = 0; i < ops.size(); i++)
    { OperationDescription od = (OperationDescription) ops.get(i);
      String odname = od.getODName(); 
      res = res + "    String " + odname + "C = req.getParameter(\"" + odname + 
            "\");\n" + 
            "    if (" + odname + "C != null)\n" + 
            "    { pw.println(new " + odname + "Page()); }\n"; 
    }
    res = res + "    pw.close();\n" + 
      "  }\n\n";

    res = res +
      "  public void doPost(HttpServletRequest req,\n" + 
      "               HttpServletResponse res)\n" +
      "  throws ServletException, IOException\n" +
      "  { doGet(req,res); }\n\n"; 

    res = res + "}\n";
    return res;
  }

  public String generateErrorPage()
  { String res = "public class ErrorPage extends BasePage\n" +
                 "{ private int errors = 0; \n" + 
                 "  HtmlItem para = new HtmlItem(\"p\");\n\n" +
                 "  public void addMessage(String t)\n" + 
                 "  { body.add(new HtmlText(t,\"strong\"));\n" +
                 "    body.add(para);\n" + 
                 "    errors++;\n" + 
                 "  }\n\n" + 
                 "  public boolean hasError() { return errors > 0; }\n" + 
                 "}\n"; 
    return res; 
  }  

  public String getSessionBeans(Vector ents, Vector ucs)
  { String res = "";
    for (int i = 0; i < ents.size(); i++)
    { Entity e = (Entity) ents.get(i);
      Vector eucs = new Vector();
      for (int j = 0; j < ucs.size(); j++)
      { OperationDescription uc = 
          (OperationDescription) ucs.get(j);
        if (uc.getEntity() == e)
        { eucs.add(uc); }
      }
      res = res + e.getSessionBean(eucs) + "\n\n";
    }
    return res;
  }

  public void addBetween(Association ast,
                         Association ast1, Association ast2,
                         Entity e)
  { LineData ld = null;
 
    for (int i = 0; i < visuals.size(); i++)
    { VisualData vd = (VisualData) visuals.elementAt(i);
      ModelElement me = vd.getModelElement(); 
      if (me == ast && (vd instanceof LineData))
      { ld = (LineData) vd; } 
    }
    if (ld != null)
    { int x1 = ld.getx(); 
      int x2 = ld.getx2(); 
      int y1 = ld.gety(); 
      int y2 = ld.gety2(); 
      int midx = (x1 + x2)/2; 
      int midy = (y1 + y2)/2; 

      RectData rd = new RectData(midx,midy,getForeground(),
                                 componentMode,
                                 rectcount);
      rectcount++;
      rd.setLabel(e.getName());
      rd.setModelElement(e); 
      visuals.add(rd);
      visuals.remove(ld);  
      LineData sline1 = 
        new LineData(x1,y1,midx,midy+5,linecount,SOLID);
      sline1.setModelElement(ast1); 
      if (midx < x2) 
      { midx = midx + rd.width - 5; } 
      // Only works left to right      

      LineData sline2 = 
        new LineData(midx,midy+5,x2,y2,linecount,SOLID);
      sline2.setModelElement(ast2); 
      visuals.add(sline1); 
      visuals.add(sline2); 
      entities.add(e); 
      associations.add(ast1); 
      associations.add(ast2); 
    }
    else 
    { System.err.println("Cannot find line for " + ast); }
  }

  public void redirectAssociation(Entity e, Association ast)
  { LineData ld = (LineData) getVisualOf(ast); 
    RectData rd = (RectData) getVisualOf(e); 
    int x = rd.getx(); 
    int y = rd.gety(); 
    ld.setx1(x + 35);   // rd.width - 5
    ld.sety1(y + 5); 
  }     

  public void redirectAssociation(Association ast, Entity e)
  { LineData ld = (LineData) getVisualOf(ast); 
    RectData rd = (RectData) getVisualOf(e); 
    int x = rd.getx(); 
    int y = rd.gety(); 
    ld.setx2(x + 5); 
    ld.sety2(y + 5); 
  }     

  public void convertStatecharts()
  { for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      Statemachine sm = e.getStatechart(); 
      if (sm == null) { } 
      else 
      { convertToCD(sm,e); } 
    } 
  } 

  private void convertToCD(Statemachine sm, Entity ent)
  { // generates CD equivalents of sm
    Vector states = sm.getStateNames();
    Vector transitions = sm.getTransitions(); 
    System.out.println("States are: " + states); 
 
    if (states == null || states.size() < 1) { return; } 
    State ini = sm.getInitial();
    String ename = ent.getName();  
    // Build an enumerated type and add it:
    String tname = ename + "States"; 
    Type x = (Type) ModelElement.lookupByName(tname,types); 
    if (x != null) 
    { System.err.println("Redefining existing type"); 
      types.remove(x); // and from the visuals
      VisualData vd = getVisualOf(x); 
      visuals.remove(vd); 
    }
    Type t = new Type(tname,states);
    types.add(t); 
    RectData rd = new RectData(10, 10 + 40*types.size(),
                               getForeground(),componentMode,rectcount); 
    rectcount++; 
    rd.setLabel(tname);  
    rd.setModelElement(t); 
    visuals.add(rd); 

    String attname = ename.toLowerCase() + "State";       
    Attribute att = 
      new Attribute(attname,t,ModelElement.INTERNAL);
    att.setEntity(ent); 
 
    if (ini != null)
    { att.setInitialValue(ini.label);
      Vector contexts = new Vector(); 
      contexts.add(ent); 
        
      Expression init = new BasicExpression(ini.label);
      init.typeCheck(types,entities,contexts,new Vector()); 
      att.setInitialExpression(init); 
    } 
    ent.removeAttribute(attname); 
    ent.addAttribute(att);
    ent.addStateInvariants(att,sm.getStates(),types,entities); 
 
    Vector gbls = 
      ent.addTransitionConstraints(transitions,types,entities); // type-check them
    for (int j = 0; j < gbls.size(); j++) 
    { Constraint c = (Constraint) gbls.get(j); 
      Entity cown = c.getOwner(); 
      Vector context = new Vector(); 
      context.add(cown); 
        
      boolean ok = c.typeCheck(types,entities,context); 
      if (ok)
      { constraints.add(c); } 
      else 
      { System.out.println("Type error in: " + c); } 
    } 
    // System.out.println(sm.loopStates());     

    repaint(); 
  } 

  public Vector getOperations()
  { Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++)
    { Entity ent = (Entity) entities.get(i);
      res.addAll(ent.getOperations()); 
    } 
    return res; 
  } 

  public Association findLinkingAssociation(Entity e1, Entity e2)
  { // either e1 --> e2 directly, or between superclasses of e1 and e2
    Entity ent1 = e1; 
    while (ent1 != null)
    { Vector assocs = ent1.getAssociations(); 
      for (int i = 0; i < assocs.size(); i++) 
      { Association a = (Association) assocs.get(i); 
        Entity ent2 = a.getEntity2(); 
        if (ent2 == e2)
        { return a; } 
        if (Entity.isAncestor(ent2,e2))
        { return a.generateSubAssociation(e1,e2); } 
      }
      ent1 = ent1.getSuperclass(); 
    }
    return null; 
  }

  public Association findSubclassAssociation(Entity e1, Vector rels)
  { // generate subassoc of a : rels with e1 as source
     
    for (int i = 0; i < rels.size(); i++) 
    { Association a = (Association) rels.get(i); 
      Entity ent1 = a.getEntity1(); 
      if (ent1 == e1)
      { return a; } 
      if (Entity.isAncestor(ent1,e1))
      { rels.remove(a); 
        return a.generateSubAssociation(e1,a.getEntity2()); 
      }   
    }
    return null; 
  }

  public Association findSubclass2Association(Entity e2, Vector rels)
  { // generate subassoc of a : rels with e2 as target
     
    for (int i = 0; i < rels.size(); i++) 
    { Association a = (Association) rels.get(i); 
      Entity ent2 = a.getEntity2(); 
      if (ent2 == e2)
      { return a; } 
      if (Entity.isAncestor(ent2,e2))
      { rels.remove(a); 
        return a.generateSubAssociation(a.getEntity1(),e2);
      }   
    }
    return null; 
  }

  /* Models must be completely specified, with inverse association 
     end values defined */ 

  private String getLoadModelOp()
  { String sysName = ""; 
    if (systemName != null && systemName.length() > 0) 
    { sysName = systemName + "."; } 

    String res = "  public static void loadModel(String file)\n" + 
    "  {\n" + 
    "    try\n" +
    "    { BufferedReader br = null;\n" +
    "      File f = new File(file);\n" +
    "      try \n" +
    "      { br = new BufferedReader(new FileReader(f)); }\n" +
    "      catch (Exception ex) \n" +
    "      { System.err.println(\"No file: \" + file); return; }\n" +
    "      Class cont = Class.forName(\"" + sysName + "Controller\");\n" +
    "      java.util.Map objectmap = new java.util.HashMap();\n" +
    "      while (true)\n" +
    "      { String line1;\n" +
    "        try { line1 = br.readLine(); }\n" +
    "        catch (Exception e)\n" +
    "        { return; }\n" +
    "        if (line1 == null)\n" + 
    "        { return; }\n" +
    "        line1 = line1.trim();\n\n" + 
    "        if (line1.length() == 0) { continue; }\n" + 
    "        if (line1.startsWith(\"//\")) { continue; }\n" + 
    "        String left;\n" + 
    "        String op;\n" + 
    "        String right;\n" + 
    "        if (line1.charAt(line1.length() - 1) == '\"')\n" + 
    "        { int eqind = line1.indexOf(\"=\"); \n" + 
    "          if (eqind == -1) { continue; }\n" +  
    "          else \n" + 
    "          { left = line1.substring(0,eqind-1).trim();\n" +  
    "            op = \"=\"; \n" + 
    "            right = line1.substring(eqind+1,line1.length()).trim();\n" + 
    "          }\n" +  
    "        }\n" +  
    "        else\n" +    
    "        { StringTokenizer st1 = new StringTokenizer(line1);\n" +
    "          Vector vals1 = new Vector();\n" +
    "          while (st1.hasMoreTokens())\n" +
    "          { String val1 = st1.nextToken();\n" +
    "            vals1.add(val1);\n" +
    "          }\n" +
    "          if (vals1.size() < 3)\n" +
    "          { continue; }\n" +
    "          left = (String) vals1.get(0);\n" +
    "          op = (String) vals1.get(1);\n" +
    "          right = (String) vals1.get(2);\n" +
    "        }\n" + 
    "        if (\":\".equals(op))\n" +
    "        { int i2 = right.indexOf(\".\");\n" +
    "          if (i2 == -1)\n" +
    "          { Class cl;\n" + 
    "            try { cl = Class.forName(\"" + sysName + "\" + right); }\n" +
    "            catch (Exception _x) { System.err.println(\"No entity: \" + right); continue; }\n" + 
    "            Object xinst = cl.newInstance();\n" +
    "            objectmap.put(left,xinst);\n" +
    "            Class[] cargs = new Class[] { cl };\n" + 
    "            Method addC = null;\n" + 
    "            try { addC = cont.getMethod(\"add\" + right,cargs); }\n" +
    "            catch (Exception _xx) { System.err.println(\"No entity: \" + right); continue; }\n" + 
    "            if (addC == null) { continue; }\n" + 
    "            Object[] args = new Object[] { xinst };\n" + 
    "            addC.invoke(Controller.inst(),args);\n" +
    "          }\n" +
    "          else\n" + 
    "          { String obj = right.substring(0,i2);\n" + 
    "            String role = right.substring(i2+1,right.length());\n" +  
    "            Object objinst = objectmap.get(obj); \n" +
    "            if (objinst == null) \n" +
    "            { continue; }\n" + 
    "            Object val = objectmap.get(left);\n" + 
    "            if (val == null) \n" +
    "            { continue; }\n" + 
    "            Class objC = objinst.getClass();\n" + 
    "            Class typeclass = val.getClass(); \n" +
    "            Object[] args = new Object[] { val }; \n" +
    "            Class[] settypes = new Class[] { typeclass };\n" + 
    "            Method addrole = Controller.findMethod(objC,\"add\" + role);\n" + 
    "            if (addrole != null) \n" +
    "            { addrole.invoke(objinst, args); }\n" +
    "            else { System.err.println(\"Error: cannot add to \" + role); }\n" +  
    "          }\n" +
    "        }\n" +
    "        else if (\"=\".equals(op))\n" +
    "        { int i1 = left.indexOf(\".\");\n" + 
    "          if (i1 == -1) \n" +
    "          { continue; }\n" + 
    "          String obj = left.substring(0,i1);\n" + 
    "          String att = left.substring(i1+1,left.length());\n" + 
    "          Object objinst = objectmap.get(obj); \n" +
    "          if (objinst == null) \n" +
    "          { System.err.println(\"No object: \" + obj); continue; }\n" + 
    "          Class objC = objinst.getClass();\n" + 
    "          Class typeclass; \n" +
    "          Object val; \n" +
    "          if (right.charAt(0) == '\"' &&\n" +
    "              right.charAt(right.length() - 1) == '\"')\n" +
    "          { typeclass = String.class;\n" +
    "            val = right.substring(1,right.length() - 1);\n" +
    "          } \n" +
    "          else if (\"true\".equals(right) || \"false\".equals(right))\n" +
    "          { typeclass = boolean.class;\n" +
    "            if (\"true\".equals(right))\n" +
    "            { val = new Boolean(true); }\n" +
    "            else\n" +
    "            { val = new Boolean(false); }\n" +
    "          }\n" +
    "          else \n" +
    "          { val = objectmap.get(right);\n" +
    "            if (val != null)\n" +
    "            { typeclass = val.getClass(); }\n" +
    "            else \n" +
    "            { int i;\n" +
    "              long l; \n" + 
    "              double d;\n" +
    "              try \n" +
    "              { i = Integer.parseInt(right);\n" + 
    "                typeclass = int.class;\n" +
    "                val = new Integer(i); \n" +
    "              }\n" +
    "              catch (Exception ee)\n" +
    "              { try \n" +
    "                { l = Long.parseLong(right);\n" + 
    "                  typeclass = long.class;\n" +
    "                  val = new Long(l); \n" +
    "                }\n" +
    "                catch (Exception eee)\n" +
    "                { try\n" +
    "                  { d = Double.parseDouble(right);\n" + 
    "                    typeclass = double.class;\n" +
    "                    val = new Double(d);\n" +
    "                  }\n" +
    "                  catch (Exception ff)\n" +
    "                  { continue; }\n" +
    "                }\n" + 
    "              }\n" +
    "            }\n" +
    "          }\n" +
    "          Object[] args = new Object[] { val }; \n" +
    "          Class[] settypes = new Class[] { typeclass };\n" + 
    "          Method setatt = Controller.findMethod(objC,\"set\" + att);\n" + 
    "          if (setatt != null) \n" +
    "          { setatt.invoke(objinst, args); }\n" + 
    "          else { System.err.println(\"No attribute: \" + objC.getName() + \"::\" + att); }\n" +
    "        }\n" +
    "      }\n" +
    "    } catch (Exception e) { }\n" + 
    "  }\n\n" + 
    "  public static Method findMethod(Class c, String name)\n" +
    "  { Method[] mets = c.getMethods(); \n" +
    "    for (int i = 0; i < mets.length; i++)\n" + 
    "    { Method m = mets[i];\n" + 
    "      if (m.getName().equals(name))\n" +
    "      { return m; }\n" + 
    "    } \n" + 
    "    return null;\n" +  
    "  }\n\n"; 
    return res; 
  } 

  private String getLoadModelDeltaOp()
  { String sysName = ""; 
    if (systemName != null && systemName.length() > 0) 
    { sysName = systemName + "."; } 

    String res = 
    "  static Vector _modobjs = new Vector();\n" +  
    "  static Vector _newobjs = new Vector();\n\n" +  
    "  static java.util.Map _objectmap = new java.util.HashMap();\n\n" + 
    "  public static void loadModelDelta(String file)\n" + 
    "  {\n" + 
    "    try\n" +
    "    { BufferedReader br = null;\n" +
    "      File f = new File(file);\n" +
    "      try \n" +
    "      { br = new BufferedReader(new FileReader(f)); }\n" +
    "      catch (Exception ex) \n" +
    "      { System.err.println(\"No file: \" + file); return; }\n" +
    "      Class cont = Class.forName(\"" + sysName + "Controller\");\n" +
    "      _modobjs.clear(); _newobjs.clear();\n" +
    "      while (true)\n" +
    "      { String line1;\n" +
    "        try { line1 = br.readLine(); }\n" +
    "        catch (Exception e)\n" +
    "        { return; }\n" +
    "        if (line1 == null)\n" + 
    "        { return; }\n" +
    "        line1 = line1.trim();\n\n" + 
    "        if (line1.length() == 0) { continue; }\n" + 
    "        String left;\n" + 
    "        String op;\n" + 
    "        String right;\n" + 
    "        if (line1.charAt(line1.length() - 1) == '\"')\n" + 
    "        { int eqind = line1.indexOf(\"=\"); \n" + 
    "          if (eqind == -1) { continue; }\n" +  
    "          else \n" + 
    "          { left = line1.substring(0,eqind-1).trim();\n" +  
    "            op = \"=\"; \n" + 
    "            right = line1.substring(eqind+1,line1.length()).trim();\n" + 
    "          }\n" +  
    "        }\n" +  
    "        else\n" +    
    "        { StringTokenizer st1 = new StringTokenizer(line1);\n" +
    "          Vector vals1 = new Vector();\n" +
    "          while (st1.hasMoreTokens())\n" +
    "          { String val1 = st1.nextToken();\n" +
    "            vals1.add(val1);\n" +
    "          }\n" +
    "          if (vals1.size() < 3)\n" +
    "          { continue; }\n" +
    "          left = (String) vals1.get(0);\n" +
    "          op = (String) vals1.get(1);\n" +
    "          right = (String) vals1.get(2);\n" +
    "        }\n" + 
    "        if (\":\".equals(op))\n" +
    "        { int i2 = right.indexOf(\".\");\n" +
    "          if (i2 == -1)\n" +
    "          { Class cl;\n" + 
    "            try { cl = Class.forName(\"" + sysName + "\" + right); }\n" +
    "            catch (Exception _x) { System.err.println(\"No entity: \" + right); continue; }\n" + 
    "            Object xinst = cl.newInstance();\n" +
    "            _objectmap.put(left,xinst);\n" +
    "            _newobjs.add(xinst);\n" +
    "            Class[] cargs = new Class[] { cl };\n" + 
    "            Method addC = cont.getMethod(\"add\" + right,cargs);\n" +
    "            if (addC == null) { continue; }\n" + 
    "            Object[] args = new Object[] { xinst };\n" + 
    "            addC.invoke(Controller.inst(),args);\n" +
    "          }\n" +
    "          else\n" + 
    "          { String obj = right.substring(0,i2);\n" + 
    "            String role = right.substring(i2+1,right.length());\n" +  
    "            Object objinst = _objectmap.get(obj); \n" +
    "            if (objinst == null) \n" +
    "            { continue; }\n" + 
    "            Object val = _objectmap.get(left);\n" + 
    "            if (val == null) \n" +
    "            { continue; }\n" +
    "            if (_modobjs.contains(objinst) || _newobjs.contains(objinst)) { }\n" + 
    "            else { _modobjs.add(objinst); }\n" +  
    "            Class objC = objinst.getClass();\n" + 
    "            Class typeclass = val.getClass(); \n" +
    "            Object[] args = new Object[] { val }; \n" +
    "            Class[] settypes = new Class[] { typeclass };\n" + 
    "            Method addrole = Controller.findMethod(objC,\"add\" + role);\n" + 
    "            if (addrole != null) \n" +
    "            { addrole.invoke(objinst, args); }\n" +
    "            else { System.err.println(\"Error: cannot add to \" + role); }\n" +  
    "          }\n" +
    "        }\n" +
    "        else if (\"=\".equals(op))\n" +
    "        { int i1 = left.indexOf(\".\");\n" + 
    "          if (i1 == -1) \n" +
    "          { continue; }\n" + 
    "          String obj = left.substring(0,i1);\n" + 
    "          String att = left.substring(i1+1,left.length());\n" + 
    "          Object objinst = _objectmap.get(obj); \n" +
    "          if (objinst == null) \n" +
    "          { continue; }\n" + 
    "          if (_modobjs.contains(objinst) || _newobjs.contains(objinst)) { }\n" + 
    "          else { _modobjs.add(objinst); }\n" +  
    "          Class objC = objinst.getClass();\n" + 
    "          Class typeclass; \n" +
    "          Object val; \n" +
    "          if (right.charAt(0) == '\"' &&\n" +
    "              right.charAt(right.length() - 1) == '\"')\n" +
    "          { typeclass = String.class;\n" +
    "            val = right.substring(1,right.length() - 1);\n" +
    "          } \n" +
    "          else if (\"true\".equals(right) || \"false\".equals(right))\n" +
    "          { typeclass = boolean.class;\n" +
    "            if (\"true\".equals(right))\n" +
    "            { val = new Boolean(true); }\n" +
    "            else\n" +
    "            { val = new Boolean(false); }\n" +
    "          }\n" +
    "          else \n" +
    "          { val = _objectmap.get(right);\n" +
    "            if (val != null)\n" +
    "            { typeclass = val.getClass(); }\n" +
    "            else \n" +
    "            { int i;\n" +
    "              long l; \n" + 
    "              double d;\n" +
    "              try \n" +
    "              { i = Integer.parseInt(right);\n" + 
    "                typeclass = int.class;\n" +
    "                val = new Integer(i); \n" +
    "              }\n" +
    "              catch (Exception ee)\n" +
    "              { try \n" +
    "                { l = Long.parseLong(right);\n" + 
    "                  typeclass = long.class;\n" +
    "                  val = new Long(l); \n" +
    "                }\n" +
    "                catch (Exception eee)\n" +
    "                { try\n" +
    "                  { d = Double.parseDouble(right);\n" + 
    "                    typeclass = double.class;\n" +
    "                    val = new Double(d);\n" +
    "                  }\n" +
    "                  catch (Exception ff)\n" +
    "                  { continue; }\n" +
    "                }\n" + 
    "              }\n" +
    "            }\n" +
    "          }\n" +
    "          Object[] args = new Object[] { val }; \n" +
    "          Class[] settypes = new Class[] { typeclass };\n" + 
    "          Method setatt = Controller.findMethod(objC,\"set\" + att);\n" + 
    "          if (setatt != null) \n" +
    "          { setatt.invoke(objinst, args); }\n" + 
    "          else { System.err.println(\"No attribute: \" + att); }\n" +
    "        }\n" +
    "      }\n" +
    "    } catch (Exception e) { }\n" + 
    "  }\n\n" +   
    "  public static Method findMethod(Class c, String name)\n" +
    "  { Method[] mets = c.getMethods(); \n" +
    "    for (int i = 0; i < mets.length; i++)\n" + 
    "    { Method m = mets[i];\n" + 
    "      if (m.getName().equals(name))\n" +
    "      { return m; }\n" + 
    "    } \n" + 
    "    return null;\n" +  
    "  }\n\n"; 

    return res; 
  } 

  public void createDateComponent()
  { Entity e = new Entity("Date"); 
    e.addStereotype("external"); 
    BehaviouralFeature op = 
      new BehaviouralFeature("getTime",new Vector(),true,new Type("double",null)); 
    e.addOperation(op); 
    entities.add(e);                           
    RectData rd = new RectData(300,10,getForeground(),
                               componentMode,
                               rectcount);
    rectcount++;
    rd.setLabel(e.getName());
    rd.setModelElement(e); 
    visuals.add(rd);
    repaint();           
  } 

  public void createXMLParserComponent()
  { Entity e = new Entity("XMLParser"); 
    e.addStereotype("external"); 
    Entity x = new Entity("XMLNode"); 
    // x.addStereotype("external");
    Attribute tag = new Attribute("tag",new Type("String",null),ModelElement.INTERNAL); 
    tag.setElementType(new Type("String", null)); 
    x.addAttribute(tag); 
    tag.setEntity(x); 

    Entity xatt = new Entity("XMLAttribute"); 
    // xatt.addStereotype("external");
    Attribute xattname = new Attribute("name",new Type("String",null),ModelElement.INTERNAL); 

    xattname.setElementType(new Type("String", null));     
    xatt.addAttribute(xattname); 
    xattname.setEntity(xatt); 
    Attribute xattvalue = new Attribute("value",new Type("String",null),ModelElement.INTERNAL); 

    xattvalue.setElementType(new Type("String", null)); 
    xatt.addAttribute(xattvalue);
    xattvalue.setEntity(xatt);  

    Attribute par = new Attribute("f",new Type("String",null),ModelElement.INTERNAL); 
    Vector pars = new Vector(); 
    pars.add(par);  
    BehaviouralFeature op = 
      new BehaviouralFeature("parseXML",pars,true,new Type(x)); 
    e.addOperation(op);
    op.setInstanceScope(false);  

    Vector pars1 = new Vector(); 
    BehaviouralFeature op1 = 
      new BehaviouralFeature("allXMLNodes",pars1,true,new Type("Set",null)); 
    op1.setElementType(new Type(x)); 
    e.addOperation(op1);
    op1.setInstanceScope(false);  

    BehaviouralFeature op2 = 
      new BehaviouralFeature("allXMLAttributes",pars1,true,new Type("Set",null)); 
    op2.setElementType(new Type(xatt)); 
    e.addOperation(op2);
    op2.setInstanceScope(false);  

    entities.add(e);    
    entities.add(x);                        
    entities.add(xatt); 

    RectData rd = new RectData(270,10,getForeground(),
                               componentMode,
                               rectcount);
    rectcount++;
    rd.setLabel(e.getName());
    rd.setModelElement(e); 
    visuals.add(rd);

    RectData rdx = new RectData(420,10,getForeground(), componentMode, rectcount);
    rectcount++;
    rdx.setLabel(x.getName());
    rdx.setModelElement(x); 
    visuals.add(rdx);

    RectData rdxatt = new RectData(420,100,getForeground(),
                               componentMode,
                               rectcount);
    rectcount++;
    rdxatt.setLabel(xatt.getName());
    rdxatt.setModelElement(xatt); 
    visuals.add(rdxatt);

    Association a1 = new Association(x,xatt,ModelElement.ZEROONE, 
                                     ModelElement.MANY,null,"attributes"); 
    LineData sline1 = 
        new LineData(429,47,432,102,linecount,SOLID);
    sline1.setModelElement(a1); 
    // XMLNode XMLAttribute -1 429 47 432 102 0 attributes null 
    linecount++; 
    visuals.add(sline1); 
    associations.add(a1); 

    Association a2 = new Association(x,x,ModelElement.ZEROONE, 
                                     ModelElement.MANY,null,"subnodes");
    LineData sline2 = 
        new LineData(488,20,478,47,linecount,SOLID);
    LinePoint p1 = new LinePoint(542,17); 
    LinePoint p2 = new LinePoint(541,64); 
    LinePoint p3 = new LinePoint(479,66); 
    sline2.addWaypoint(p1); sline2.addWaypoint(p2); sline2.addWaypoint(p3); 
    sline2.setModelElement(a2);
    linecount++;  
    // XMLNode XMLNode -1 488 20 478 47 0 subnodes null 542 17 541 64 479 66 
    visuals.add(sline2); 
    associations.add(a2); 

    repaint();           
  } 


  public void auxiliaryMetamodel()
  { // Makes a superclass $IN of sources, $OUT of targets, and $Trace class
    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 

    System.out.println("Sources: " + sources); 
    System.out.println("Targets: " + targets); 
    // System.out.println("Enter intended interpretation from sources to targets, eg: S1 > T1, S2 > T2"); 
    // String interp = 
    //   JOptionPane.showInputDialog("Enter intended interpretation from sources to targets, eg: S1 > T1, S2 > T2");
    // StringTokenizer st = new StringTokenizer(interp," >,"); 
    // Vector strs = new Vector(); 
    // while (st.hasMoreTokens())
    // { String pp = st.nextToken();
    //   strs.add(pp); 
    // } 
    
    // create new $Trace entity, add to metamodels
    Entity insup = new Entity("$IN"); 
    insup.setAbstract(true); 
    Entity outsup = new Entity("$OUT"); 
    outsup.setAbstract(true); 
    Entity trace = new Entity("$Trace"); 
    trace.addStereotype("auxiliary"); 
    addEntity(insup,20,20); 
    addEntity(outsup,600,20); 
    addEntity(trace,300,30); 
    Attribute rulename = new Attribute("rule", new Type("String",null), ModelElement.INTERNAL); 
    Attribute varname = new Attribute("variable", new Type("String",null), ModelElement.INTERNAL); 
    trace.addAttribute(rulename);
    rulename.setEntity(trace);  
    trace.addAttribute(varname); 
    varname.setEntity(trace); 

    // Vector entities = ucdArea.getEntities(); 
    Vector asts = new Vector(); 
    Vector gens = new Vector(); 

    Association srcast = new Association(insup,trace,ModelElement.ONE,ModelElement.MANY,
                                         "","$trace"); 
    asts.add(srcast); 
    srcast.setOrdered(true); 
    insup.addAssociation(srcast); 
   
    Association trgast = new Association(trace,outsup,ModelElement.MANY,ModelElement.ONE,
                                             "","target"); 
    asts.add(trgast);
    trace.addAssociation(trgast); 
      
    // for each root source entity create inheritance to $IN
    for (int i = 0; i < sources.size(); i++)
    { Entity src = (Entity) sources.get(i); 
      if (src != null && src.isRoot()) 
      { Generalisation g = new Generalisation(insup,src);
        gens.add(g);
      } 
    } 

    // for each root target entity create inheritance to $OUT
    for (int i = 0; i < targets.size(); i++)
    { Entity trg = (Entity) targets.get(i); 
      if (trg != null && trg.isRoot()) 
      { Generalisation g = new Generalisation(outsup,trg); 
        gens.add(g); 
      }   
    } 
    addAssociations(asts); 
    addGeneralisations(gens); 
  } 

  private void changedEntityName(String oldN, String newN)
  { if (oldN.equals(newN)) { return; } 
    for (int i = 0; i < useCases.size(); i++) 
    { if (useCases.get(i) instanceof UseCase)
      { UseCase uc = (UseCase) useCases.get(i); 
        uc.changedEntityName(oldN, newN); 
      } 
    } 
  } 


  public double nodeCount(Vector ents) 
  { int en = ents.size(); 
    return 1.0*(en - 1)*(en - 2); 
  } 

  public double entityLeadership(Entity e, int n, double sumvd, double nc) 
  { 
    if (nc <= 0) 
    { return 1; } 
    int vd = e.vertexDegree(); 
    return (n*vd - sumvd)/nc; 
  } 

  public double sumvertexDegrees(Vector ents) 
  { double sum = 0.0; 
    for (int i = 0; i < ents.size(); i++) 
    { Entity ex = (Entity) ents.get(i); 
      int vex = ex.vertexDegree(); 
      sum = sum + vex;  
    } 
    return sum; 
  } 

  public Vector pairwiseDisjointEdges(Vector ents)
  { Vector domain = new Vector(); 
    Vector range = new Vector(); 
    Vector res = new Vector(); 

    for (int i = 0; i < ents.size(); i++) 
    { Entity e1 = (Entity) ents.get(i);
      if (domain.contains(e1)) { continue; } 
      if (range.contains(e1)) { continue; } 
 
      Vector e1ns = e1.neighbourhood(); 
      for (int j = 0; j < e1ns.size(); j++) 
      { Entity e2 = (Entity) e1ns.get(j); 
        if (domain.contains(e2)) { continue; } 
        if (range.contains(e2)) { continue; } 
        domain.add(e1); range.add(e2); 
        res.add(new Maplet(e1,e2)); 
      } 
    }         
    return res; 
  } 

  public void computeLeadership()
  { 
    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 
    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Vector unused = new Vector(); 
    unused.addAll(entities); 
    unused.removeAll(sources); 
    unused.removeAll(targets); 

    // Vector allbmaps = Map.allMaps(sources,targets); 
    
    // System.out.println("**** EVALUATING " + allbmaps.size() + " entity mappings"); 


    double bestscore = 0; 
 
    double srclead = sumvertexDegrees(sources); 
    double trglead = sumvertexDegrees(targets); 
    double srcnc = nodeCount(sources); 
    double trgnc = nodeCount(targets); 
    Vector sedges = pairwiseDisjointEdges(sources);
    Vector tedges = pairwiseDisjointEdges(targets);  

    int nsrc = sources.size(); 
    int ntrg = targets.size(); 

    Map mm = new Map(); 
 
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      double sbest = 0; 
      Entity smatch = null; 

      double l1 = entityLeadership(ei,nsrc,srclead,srcnc);
      double b1 = ei.bonding(); 
      double d1 = ei.disjointEdgeCount(sedges); 

      for (int j = 0; j < targets.size(); j++) 
      { Entity ej = (Entity) targets.get(j); 

        double l2 = entityLeadership(ej,ntrg,trglead,trgnc);
        double b2 = ej.bonding(); 
        double d2 = ej.disjointEdgeCount(tedges); 

        double lsim = 1 - Math.abs(l1 - l2); 
        double bsim = 1 - Math.abs(b1 - b2); 
        double dsim = 1 - Math.abs(d1 - d2); 

        double sim = lsim + bsim + dsim; 
        if (sim > sbest) 
        { sbest = sim; 
          smatch = ej; 
        } 
      }
      if (smatch != null) 
      { mm.set(ei,smatch); 
        bestscore = bestscore + sbest; 
      } 
    } // create a set of maps

  /*  LexicographicOrdering lex = new LexicographicOrdering(); 
    lex.setalphabet(targets); 
    lex.init(); 
    int maxmapsize = sources.size(); 

    Vector word = new Vector(); 
    word = (Vector) lex.increment(word); 
    while (word.size() < maxmapsize) 
    { word = (Vector) lex.increment(word); }  
    // skip non-total maps

    // for (int mind = 0; mind < allbmaps.size(); mind++)
    while (word.size() == maxmapsize) 
    { // Map mm0 = (Map) allbmaps.get(mind); 
      // ModelMatching modmatch = new ModelMatching(mm,entities);
      Map mm0 = lex.getMap(word,sources);  

    // for (int mind = 0; mind < allbmaps.size(); mind++) 
    // { Map mm0 = (Map) allbmaps.get(mind); 
      Map mm = Map.extendDomainRange(mm0,unused); 
      // ModelMatching modmatch = new ModelMatching(mm,entities); 
  
      double mapscore = 0; 
      double lscore = 0; 
      double bscore = 0; 
      double dscore = 0; 


      Vector csources = mm.domain(); 
      for (int i = 0; i < csources.size(); i++) 
      { Entity ei = (Entity) csources.get(i); 
        Entity ej = (Entity) mm.get(ei); 
        
        double l1 = entityLeadership(ei,nsrc,srclead,srcnc);
        double l2 = entityLeadership(ej,ntrg,trglead,trgnc);

        double b1 = ei.bonding(); 
        double b2 = ej.bonding(); 

        double d1 = ei.disjointEdgeCount(sedges); 
        double d2 = ej.disjointEdgeCount(tedges); 

        double lsim = 1 - Math.abs(l1 - l2); 
        double bsim = 1 - Math.abs(b1 - b2); 
        double dsim = 1 - Math.abs(d1 - d2); 
  
        lscore = lscore + lsim;
        bscore = bscore + bsim;  
        dscore = dscore + dsim; 
        mapscore = mapscore + lsim + bsim + dsim; 
        // System.out.println("Leadership similarity of " + ei + " and " + ej + " is: " + l1 + " " + l2); 
        // System.out.println("Bonding similarity is: " + b1 + " " + b2);  
        // System.out.println("Diversity similarity is: " + d1 + " " + d2);  
      }


      if (lscore > bestLscore) 
      { bestLscore = lscore; 
        bestL = new Vector(); 
        bestL.add(mm); 
      } 
      else if (lscore > 0 && lscore == bestLscore)
      { bestL.add(mm); } 

      if (bscore > bestBscore) 
      { bestBscore = bscore; 
        bestB = new Vector(); 
        bestB.add(mm); 
      } 
      else if (bscore > 0 && bscore == bestBscore)
      { bestB.add(mm); } 

      if (dscore > bestDscore) 
      { bestDscore = dscore; 
        bestD = new Vector(); 
        bestD.add(mm); 
      } 
      else if (dscore > 0 && dscore == bestDscore)
      { bestD.add(mm); } 

      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        best = new Vector(); 
        best.add(mm); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm); 
        System.out.println("=== Similarity map score is " + mapscore);  
        System.out.println(); 
      } 
      else if (mapscore > 0 && mapscore == bestscore)
      { best.add(mm); } 
      word = (Vector) lex.increment(word); 
    }

    System.out.println("===== The best map(s) via leadership are " + bestL); 
    System.out.println("===== with score " + bestLscore); 
    System.out.println(); 
    System.out.println("===== The best map(s) via bonding are " + bestB); 
    System.out.println("===== with score " + bestBscore); 
    System.out.println(); 
    System.out.println("===== The best map(s) via diversity are " + bestD); 
    System.out.println("===== with score " + bestDscore); */ 

    System.out.println(); 
    System.out.println("===== The best overall map is " + mm); 
    System.out.println("===== with score " + bestscore); 
    System.out.println(); 
    
  } 

  public void nameSimilarity(Vector thesaurus)
  { 
    String compositionDepth = 
      JOptionPane.showInputDialog("Max source feature chain length? (1 or 2): ");
    int scdepth = Integer.parseInt(compositionDepth); 
    compositionDepth = 
      JOptionPane.showInputDialog("Max target feature chain length? (1 or 2): ");
    int tcdepth = Integer.parseInt(compositionDepth); 
    
    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 
    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    java.util.Date date1 = new java.util.Date(); 
    long time1 = date1.getTime(); 

    Vector unused = new Vector(); 
    unused.addAll(entities); 
    unused.removeAll(sources); 
    unused.removeAll(targets); 

    // Vector allbmaps = Map.allMaps(sources,targets); 
    
    // System.out.println("**** EVALUATING " + allbmaps.size() + " entity mappings"); 

    Map mm = new Map(); 
    double bestscore = 0; 
    
    Map mmalternatives = new Map(); 

  /* 
    for (int i = 0; i < sources.size(); i++) 
    { Entity se = (Entity) sources.get(i); 
      String sename = se.getName(); 
      double sbest = 0; 
      Entity smatch = null; 
      Vector altmatches = new Vector(); 

      for (int j = 0; j < targets.size(); j++) 
      { Entity te = (Entity) targets.get(j); 
        String tename = te.getName(); 
        double sim = ModelElement.similarity(sename.toLowerCase(), tename.toLowerCase()); 
        if (sim > sbest) 
        { sbest = sim; 
          smatch = te; 
          altmatches.clear(); 
          altmatches.add(te); 
        } 
        else if (sim > 0 && sim == sbest) 
        { altmatches.add(te); } 
      }
      if (smatch != null) 
      { mm.set(se,smatch); 
        bestscore = bestscore + sbest; 
        mmalternatives.set(se,altmatches); 
      } 
    } // create a set of maps
       */ 

    for (int i = 0; i < sources.size(); i++) 
    { Entity se = (Entity) sources.get(i); 
      String sename = se.getName(); 
      String slcname = sename.toLowerCase(); 

      double sbest = 0; 
      Entity smatch = null; 
      Vector salternatives = new Vector(); 

      for (int j = 0; j < targets.size(); j++) 
      { Entity te = (Entity) targets.get(j); 
        String tename = te.getName(); 
        String tlcname = tename.toLowerCase(); 

        boolean concrete2abstract = false; 
        Vector ctargets = new Vector(); 

        double sim = 0; 
        if (se.isConcrete() && te.isAbstract())
        { concrete2abstract = true; } 
        sim = ModelElement.similarity(slcname, tlcname); 
        System.out.println(">>> NSS of " + se + " " + te + " is: " + sim); 
 
        if (sim > sbest) 
        { sbest = sim; 
          salternatives.clear(); 
          if (concrete2abstract) 
          { ctargets = te.getAllConcreteSubclasses(); 
            for (int h = 0; h < ctargets.size(); h++) 
            { Entity ctarg = (Entity) ctargets.get(h); 
              if (!salternatives.contains(ctarg))
              { salternatives.add(ctarg); }   
            }
          }  
          else if (salternatives.contains(te)) { } 
          else  
          { salternatives.add(te); }  
          smatch = te; 
        } 
        else if (sim == sbest) 
        { if (concrete2abstract) 
          { ctargets = te.getAllConcreteSubclasses(); 
            for (int h = 0; h < ctargets.size(); h++) 
            { Entity ctarg = (Entity) ctargets.get(h); 
              if (!salternatives.contains(ctarg))
              { salternatives.add(ctarg); } 
            }  
          } 
          else if (salternatives.contains(te)) { } 
          else  
          { salternatives.add(te); }         
        } 
     
        if (smatch != null) 
        { mm.set(se,smatch); 
          bestscore = bestscore + sbest; 
          mmalternatives.set(se,salternatives); 
        } 
      }
    } 

    System.out.println("===== The best map is " + mm); 
    System.out.println("===== with score " + bestscore); 
    System.out.println("===== Alternatives are: " + mmalternatives); 

    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      ei.defineLocalFeatures(); 
      if (scdepth > 1)
      { ei.defineNonLocalFeatures(); }  
    } 

    for (int i = 0; i < targets.size(); i++) 
    { Entity ei = (Entity) targets.get(i); 
      ei.defineLocalFeatures(); 
      if (tcdepth > 1)
      { ei.defineNonLocalFeatures(); }  
    } 

    for (int i = 0; i < unused.size(); i++) 
    { Entity ei = (Entity) unused.get(i); 
      ei.defineLocalFeatures(); 
      if (scdepth > 1)
      { ei.defineNonLocalFeatures(); }  
    } 

    Map alternativesmap = new Map(mmalternatives); 
    LexMultiOrdering lex = new LexMultiOrdering(alternativesmap); 

    System.out.println("===== Alternative maps are " + alternativesmap); 
    System.out.println("===== Lex: " + lex); 
    Vector msources = alternativesmap.domain(); 
    System.out.println("===== msources: " + msources); 
    int maxsize = msources.size(); 

    Vector word = new Vector(); 
    word = (Vector) lex.increment(maxsize,word);

    // if (injectiveOnly == false)  
    // { while (word.size() < maxsize) 
    //   { System.out.println(">>> skipping " + word); 
    //     word = (Vector) lex.increment(maxsize,word); 
    //   } 
    // } 

    ModelMatching modmatch = null; 
    bestscore = 0; 

    while (word.size() < maxsize) 
    { word = (Vector) lex.increment(maxsize,word); } 
    
    while (word.size() == maxsize)
    { // System.out.println(">>> processing " + word); 

      Map newmap = LexMultiOrdering.getMap(word,msources); 

      Map mm1 = Map.extendDomainRange(newmap,unused); 
      ModelMatching newmodmatch = new ModelMatching(mm1,entities);  
      
      double mapscore = 0; 
      double factor = 1; 

      if (newmodmatch.isMonotonic()) { } 
      else 
      { System.out.println(">> non-monotonic map: " + mm1); 
        factor = 0; 
      } 


      Vector csources = mm1.domain(); 
      System.out.println(">>> Processing map: " + mm1); 

      for (int j = 0; j < csources.size(); j++) 
      { Entity ei = (Entity) csources.get(j); 
        Entity ej = (Entity) newmap.get(ei); 
        if (ei != null && ej != null) 
        { if (ei.isConcrete() && ej.isAbstract())
          { factor = 0; } 
          else 
          { mapscore = mapscore + 
                       ei.esimForNSSNMS(ej,mm1,newmodmatch,entities,thesaurus);
          }
        } 
      } 

      mapscore = mapscore*factor; 

      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        modmatch = newmodmatch; 
      }  


      word = (Vector) lex.increment(maxsize,word); 
    } 

    java.util.Date date2 = new java.util.Date(); 
    long time2 = date2.getTime(); 
    System.out.println(">>> Time taken = " + (time2 - time1)); 

    System.out.println(">>> Model matching is: "); 
    System.out.println(modmatch); 
    synthesiseTransformations(modmatch,entities);
  } 

  public void ontologicalSimilarity(Vector thesaurus)
  { 
    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 
    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Vector concretesources = new Vector(); 
    Vector originalentities = new Vector(); 
    originalentities.addAll(entities); 

    String compositionDepth = 
      JOptionPane.showInputDialog("Max source feature chain length? (1 or 2): ");
    int scdepth = Integer.parseInt(compositionDepth); 
    
    
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      ei.defineLocalFeatures(); 
      if (scdepth > 1)
      { ei.defineNonLocalFeatures(); }  
    } 

    compositionDepth = 
      JOptionPane.showInputDialog("Max target feature chain length? (1 or 2): ");
    int tcdepth = Integer.parseInt(compositionDepth); 
      
    for (int i = 0; i < targets.size(); i++) 
    { Entity ei = (Entity) targets.get(i); 
      ei.defineLocalFeatures(); 
      if (tcdepth > 1)
      { ei.defineNonLocalFeatures(); }  
    } 

    Vector unused = new Vector(); 
    unused.addAll(entities); 
    unused.removeAll(sources); 
    unused.removeAll(targets); 

    for (int i = 0; i < unused.size(); i++) 
    { Entity ei = (Entity) unused.get(i); 
      ei.defineLocalFeatures(); 
      if (scdepth > 1)
      { ei.defineNonLocalFeatures(); }  
    } 

    System.out.println("**** Shared entities: " + unused); 

    Vector emapsources = new Vector(); 
    Vector emaptargets = new Vector();

    System.out.println("**** Assumed mappings: " + entitymaps); 
 
    for (int i = 0; i < entitymaps.size(); i++) 
    { EntityMatching em = (EntityMatching) entitymaps.get(i); 
      emapsources.add(em.realsrc); 
      emaptargets.add(em.realtrg);  
    } 
    sources.removeAll(emapsources); 


    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      if (ei.isConcrete()) 
      { concretesources.add(ei); } 
    } 
    concretesources.removeAll(emapsources); 


    // Vector allbmaps = Map.allMaps(sources,targets); 
    
    System.out.println("**** EVALUATING maps from " + sources + " to " + targets); 

    double bestscore = 0; 
    Vector best = new Vector(); 

    LexicographicOrdering lex = new LexicographicOrdering(); 
    lex.setalphabet(targets); 
    lex.init(); 

    int maxmapsize = sources.size(); 
    // int minmapsize = targets.size(); 


    Vector word = new Vector(); 
    word = (Vector) lex.increment(word); 

    System.out.println("*** First word: " + word); 

    // while (word.size() < minmapsize) 
    // { word = (Vector) lex.increment(word); }  
    // skip non-total maps

    // System.out.println("*** First total word: " + word); 

    // for (int mind = 0; mind < allbmaps.size(); mind++)
    while (word.size() <= maxmapsize) 
    { // Map mm0 = (Map) allbmaps.get(mind); 
      Map mm0 = lex.getMap(word,sources);  
      //  System.out.println("========= Basic entity mapping " + mm0); 
      Map mm = Map.extendDomainRange(mm0,unused); 
      //  System.out.println("========= Entity mapping extended with copies: " + mm); 
      Map mm1 = Map.extendDomainRange(mm,emapsources,emaptargets); 
      ModelMatching modmatch = new ModelMatching(mm1);
      
      double mapscore = 0; 
      double factor = 1; 

      Vector emaps = mm1.getElements(); 
      for (int i = 0; i < emaps.size(); i++)
      { Maplet emap = (Maplet) emaps.get(i); 
        Entity src = (Entity) emap.source; 
        Entity trg = (Entity) emap.dest; 
        if (src.isConcrete() && trg.isAbstract()) 
        { double nmssim = src.nmsSimilarity(trg,thesaurus);
          double nsssim = ModelElement.similarity(src.getName().toLowerCase(), 
                                                  trg.getName().toLowerCase()); 
          if (nmssim > 0.7) 
          { System.out.println("=== Similar abstract/concrete classes: " + 
                               src + " --> " + trg); 
          } 
          else if (nsssim > 0.7) 
          { System.out.println("=== Similar abstract/concrete classes: " + 
                               src + " --> " + trg); 
          }      
          factor = 0; 
        } 
        else if (factor > 0) 
        { double srctrgsim = src.cotopySimilarity(trg,mm1,modmatch,entities);
          // System.out.println("==== Cotopy similarity of " + src + " " + trg + " is " + srctrgsim); 
          mapscore = mapscore + srctrgsim; 
        } 
      } 

      mapscore = mapscore*factor; 
      
      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        best = new Vector(); 
        best.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Ontological similarity map score is " + mapscore);  
        System.out.println(); 
      } 
      else if (mapscore > 0 && mapscore == bestscore)
      { best.add(modmatch); } 

      word = (Vector) lex.increment(word); 
    } 

    System.out.println("===== The best map(s) are " + best); 
    System.out.println("===== with score " + bestscore); 
    System.out.println(); 

    if (best.size() == 0) 
    { return; } 

    ModelMatching selected = (ModelMatching) best.get(0); 
    double snamesim = selected.nameSimilarity(); 
    System.out.println("==== Name similarity of " + selected.mymap + " is " + snamesim); 
      
    for (int j = 1; j < best.size(); j++) 
    { ModelMatching bb = (ModelMatching) best.get(j); 
      double nsimbb = bb.nameSimilarity(); 
      System.out.println("==== Name similarity of " + bb.mymap + " is " + nsimbb); 
      if (bb.size() < selected.size())
      { selected = bb; } 
      else if (nsimbb > snamesim)
      { selected = bb; } 
    } 

    System.out.println("===== The selected map is " + selected);
    System.out.println(); 

    if (selected != null)
    { synthesiseTransformations(selected,entities); } 
  } 

  public void refinementScore(Vector thesaurus)
  { 
    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 

    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    String compositionDepth = JOptionPane.showInputDialog("Max feature chain length? (1 or 2): ");
    int cdepth = Integer.parseInt(compositionDepth); 

    Vector concretesources = new Vector(); 
    Vector originalentities = new Vector(); 
    originalentities.addAll(entities); 
    
    for (int i = 0; i < entities.size(); i++) 
    { Entity ei = (Entity) entities.get(i); 
      ei.defineLocalFeatures(); 
      if (cdepth > 1) 
      { ei.defineNonLocalFeatures(); }  
    } 
    
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      if (ei.isConcrete()) 
      { concretesources.add(ei); } 
    } 
      

    Vector unused = new Vector(); 
    unused.addAll(entities); 
    unused.removeAll(sources); 
    unused.removeAll(targets); 

    Vector emapsources = new Vector(); 
    Vector emaptargets = new Vector();
 
    for (int i = 0; i < entitymaps.size(); i++) 
    { EntityMatching em = (EntityMatching) entitymaps.get(i); 
      emapsources.add(em.realsrc); 
      emaptargets.add(em.realtrg);  
    } 
    sources.removeAll(emapsources); 
    concretesources.removeAll(emapsources); 

    // Vector allbmaps = Map.allMaps(sources,targets); 

    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 
    
    System.out.println("**** EVALUATING maps from " + sources + " to " + targets); 

    double bestscore = 0; 
    Vector best = new Vector(); 
    double bestabscore = 0; 
    Vector bestab = new Vector(); 
    double bestbxscore = 0; 
    Vector bestbx = new Vector(); 

    LexicographicOrdering lex = new LexicographicOrdering(); 
    lex.setalphabet(targets); 
    lex.init(); 

    int maxmapsize = sources.size(); 
    // int minmapsize = targets.size(); 


    Vector word = new Vector(); 
    word = (Vector) lex.increment(word); 

    // while (word.size() < minmapsize) 
    // { word = (Vector) lex.increment(word); }  
    // skip non-total maps

    // for (int mind = 0; mind < allbmaps.size(); mind++)
    while (word.size() <= maxmapsize) 
    { // Map mm0 = (Map) allbmaps.get(mind); 
      Map mm0 = lex.getMap(word,sources);  
      Map mm = Map.extendDomainRange(mm0,unused); 
      Map mm1 = Map.extendDomainRange(mm,emapsources,emaptargets); 
      ModelMatching modmatch = new ModelMatching(mm1);
      
      double mapscore = 0; 
      double mapabscore = 0; 
      double mapbxscore = 0; 
      double factor = 1; 

      Vector emaps = mm1.getElements(); 
      for (int i = 0; i < emaps.size(); i++)
      { Maplet emap = (Maplet) emaps.get(i); 
        Entity src = (Entity) emap.source; 
        Entity trg = (Entity) emap.dest; 

        if (src.isConcrete() && trg.isAbstract()) 
        { factor = 0; } // skip this map
        else 
        { double refscore = src.esimN(trg,mm1,modmatch,entities,thesaurus);
          mapscore = mapscore + refscore; 
          double abscore = src.esimAbsN(trg,mm1,modmatch,entities,thesaurus);
          mapabscore = mapabscore + abscore; 
          mapbxscore = mapbxscore + refscore*abscore; 
        } 
      } 

      mapscore = mapscore*factor; 
      mapabscore = mapabscore*factor; 
      mapbxscore = mapbxscore*factor; 
      
      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        best = new Vector(); 
        best.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Refinement map score is " + mapscore);  
        System.out.println(); 
      } 
      else if (mapscore > 0 && mapscore == bestscore)
      { best.add(modmatch); } 

      if (mapabscore > bestabscore) 
      { bestabscore = mapabscore; 
        bestab = new Vector(); 
        bestab.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Abstraction map score is " + mapabscore);  
        System.out.println(); 
      } 
      else if (mapabscore > 0 && mapabscore == bestabscore)
      { bestab.add(modmatch); } 

      if (mapbxscore > bestbxscore) 
      { bestbxscore = mapbxscore; 
        bestbx = new Vector(); 
        bestbx.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Bx map score is " + mapbxscore);  
        System.out.println(); 
      } 
      else if (mapbxscore > 0 && mapbxscore == bestbxscore)
      { bestbx.add(modmatch); } 


      word = (Vector) lex.increment(word); 
    } 

    System.out.println("===== The best map(s) wrt refinement are " + best); 
    System.out.println("===== with score " + bestscore); 
    System.out.println(); 

    System.out.println("===== The best map(s) wrt abstraction are " + bestab); 
    System.out.println("===== with score " + bestabscore); 
    System.out.println(); 

    System.out.println("===== The best map(s) wrt bx are " + bestbx); 
    System.out.println("===== with score " + bestbxscore); 
    System.out.println(); 

    if (bestbx.size() == 0) 
    { return; } 

    ModelMatching selected = (ModelMatching) bestbx.get(0); 
    double snamesim = selected.nameSimilarity(); 
    System.out.println("=== Name similarity of " + selected.mymap + " is " + snamesim); 
      
    for (int j = 1; j < bestbx.size(); j++) 
    { ModelMatching bb = (ModelMatching) bestbx.get(j); 
      double nsimbb = bb.nameSimilarity(); 
      System.out.println("=== Name similarity of " + bb.mymap + " is " + nsimbb); 
      if (bb.size() < selected.size())
      { selected = bb; } 
      else if (nsimbb > snamesim)
      { selected = bb; } 
    } 

    System.out.println("===== The selected map is " + selected);
    System.out.println(); 

    if (selected != null)
    { Vector csources = selected.mymap.domain(); 
      for (int i = 0; i < csources.size(); i++) 
      { Entity ei = (Entity) csources.get(i); 
        Entity ej = (Entity) selected.mymap.get(ei); 
        
        double dd = ei.compositeSimilarity(ej,selected.mymap,selected,entities);
      } 
      synthesiseTransformations(selected,entities); 
    }
  } 

  public void graphEditDistance()
  { 
    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 
    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Vector concretesources = new Vector(); 
    Vector originalentities = new Vector(); 
    originalentities.addAll(entities); 
    
    Map sourcegraphs = new Map();     
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      if (ei.isConcrete()) 
      { concretesources.add(ei); } 
      Map reachable = ei.reachableSubgraph(); 
      System.out.println("=== Reachable subgraph of " + ei.getName() + " is " + reachable); 
      sourcegraphs.set(ei, reachable); 
      // ei.defineLocalFeatures(); 
      // ei.defineNonLocalFeatures(); 
    } 

    Map targetgraphs = new Map(); 

    for (int j = 0; j < targets.size(); j++) 
    { Entity ej = (Entity) targets.get(j); 
      Map reachable = ej.reachableSubgraph(); 
      targetgraphs.set(ej,reachable); 
      System.out.println("=== Reachable subgraph of " + ej.getName() + " is " + reachable); 
      // ej.defineLocalFeatures(); 
      // ej.defineNonLocalFeatures(); 
    } 
      
    Vector unused = new Vector(); 
    unused.addAll(entities); 
    unused.removeAll(sources); 
    unused.removeAll(targets); 

    Vector emapsources = new Vector(); 
    Vector emaptargets = new Vector();
 
    for (int i = 0; i < entitymaps.size(); i++) 
    { EntityMatching em = (EntityMatching) entitymaps.get(i); 
      emapsources.add(em.realsrc); 
      emaptargets.add(em.realtrg);  
    } 
    sources.removeAll(emapsources); 
    concretesources.removeAll(emapsources); 

    // Vector allbmaps = Map.allMaps(sources,targets); 
    
    System.out.println("**** EVALUATING maps from " + sources + " to " + targets); 

    double bestscore = 0; 
    Vector best = new Vector(); 

    LexicographicOrdering lex = new LexicographicOrdering(); 
    lex.setalphabet(targets); 
    lex.init(); 

    int maxmapsize = sources.size(); 
    int minmapsize = concretesources.size(); 


    Vector word = new Vector(); 
    word = (Vector) lex.increment(word); 

    while (word.size() < minmapsize) 
    { word = (Vector) lex.increment(word); }  
    // skip non-total maps

    // for (int mind = 0; mind < allbmaps.size(); mind++)
    while (word.size() <= maxmapsize) 
    { // Map mm0 = (Map) allbmaps.get(mind); 
      Map mm0 = lex.getMap(word,sources);  
      Map mm = Map.extendDomainRange(mm0,unused); 
      Map mm1 = Map.extendDomainRange(mm,emapsources,emaptargets); 
      ModelMatching modmatch = new ModelMatching(mm1);
      
      double mapscore = 0; 
      double factor = 1; 

      Vector emaps = mm1.getElements(); 
      for (int i = 0; i < emaps.size(); i++)
      { Maplet emap = (Maplet) emaps.get(i); 
        Entity src = (Entity) emap.source; 
        Entity trg = (Entity) emap.dest; 
        if (src.isConcrete() && trg.isAbstract()) 
        { factor = 0; } 
        else 
        { Map srcgraph = (Map) sourcegraphs.get(src); 
          Map trggraph = (Map) targetgraphs.get(trg); 
          mapscore = mapscore + modmatch.graphSimilarity(srcgraph,trggraph,mm1,entities); 
        } 
      } 
   
      mapscore = mapscore*factor; 
      
      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        best = new Vector(); 
        best.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Graph edit similarity map score is " + mapscore);  
        System.out.println(); 
      } 
      else if (mapscore > 0 && mapscore == bestscore)
      { best.add(modmatch); } 

      word = (Vector) lex.increment(word); 
    } 

    System.out.println("===== The best map(s) are " + best); 
    System.out.println("===== with score " + bestscore); 
    System.out.println(); 

    if (best.size() == 0) 
    { return; } 

    ModelMatching selected = (ModelMatching) best.get(0); 
    double snamesim = selected.nameSimilarity(); 
    System.out.println("==== Name similarity of " + selected.mymap + " is " + snamesim); 
      
    for (int j = 1; j < best.size(); j++) 
    { ModelMatching bb = (ModelMatching) best.get(j); 
      double nsimbb = bb.nameSimilarity(); 
      System.out.println("==== Name similarity of " + bb.mymap + " is " + nsimbb); 
      if (bb.size() < selected.size())
      { selected = bb; } 
      else if (nsimbb > snamesim)
      { selected = bb; } 
    } 

    System.out.println("===== The selected map is " + selected);
    System.out.println(); 

    /* if (best.size() > 0)
    { ModelMatching modm = (ModelMatching) best.get(0); 
      synthesiseTransformations(modm,entities); 
    } */ 
  } 

  private Vector convertTo$Form(Vector maps, Map mflat) 
  { Vector res = new Vector(); 
    for (int i = 0; i < maps.size(); i++) 
    { Vector mm = (Vector) maps.get(i); 
      Vector newmm = new Vector(); 
      for (int j = 0; j < mm.size(); j++) 
      { Maplet mp = (Maplet) mm.get(j); 
        Entity src = (Entity) mp.source; 
        Entity trg = (Entity) mp.dest; 
        Entity src$ = (Entity) mflat.get(src); 
         // ModelElement.lookupByName(src.getName() + "$", entities); 
        Entity trg$ = (Entity) mflat.get(trg); 
         // ModelElement.lookupByName(trg.getName() + "$", entities); 
        Maplet mp$ = new Maplet(src$,trg$); 
        newmm.add(mp$); 
      } 
      res.add(newmm); 
    } 
    return res; 
  } 

  private Vector convertTo$Forms(Vector maps, Map mflat) 
  { Vector res = new Vector(); 
    for (int i = 0; i < maps.size(); i++) 
    { Maplet mm = (Maplet) maps.get(i); 
      Entity src = (Entity) mm.source; 
      Entity src$ = (Entity) mflat.get(src); 
      Vector mmdest = (Vector) mm.dest; 
      Vector newmm = new Vector(); 

      for (int j = 0; j < mmdest.size(); j++) 
      { Entity trg = (Entity) mmdest.get(j); 
        Entity trg$ = (Entity) mflat.get(trg); 
        if (trg$ != null) { newmm.add(trg$); }  
      } 

      if (src$ != null && newmm.size() > 0) 
      { res.add(new Maplet(src$,newmm)); }  
    } 
    return res; 
  } 

  public void nameSemanticSimilarity(Vector thesaurus)
  { 
    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 

    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Vector fents = new Vector(); 
    Map mflat = new Map(); 

    // boolean allmaps = false; 
    // boolean strict = false; 

  /*  if ("all maps".equals(kind)) // including superclasses
    { allmaps = true; } 
    else if ("inheritance-preserving".equals(kind))
    { allmaps = true; 
      strict = true; 
    } */ 

    String compositionDepth = JOptionPane.showInputDialog("Max source feature chain length? (<= 10, >= 1): ");
    int scdepth = Integer.parseInt(compositionDepth); 
    compositionDepth = JOptionPane.showInputDialog("Max target feature chain length? (<= 10, >= 1): ");
    int tcdepth = Integer.parseInt(compositionDepth);
    String inj = JOptionPane.showInputDialog("Injective or general maps? (I, G): ");
    boolean injectiveOnly = false; 
    if ("I".equals(inj))
    { injectiveOnly = true; } 
   
    for (int i = 0; i < sources.size(); i++) 
    { Entity ent = (Entity) sources.get(i); 
      VisualData vd = getVisualOf(ent); 
      RectData rd0 = (RectData) vd; 
      Entity fent = ent.makeFlattenedCopy(true,scdepth); 

      if (rd0 != null) 
      { RectData rd = new RectData(rd0.getx(), rd0.gety() + 400,
                                 getForeground(),componentMode,rectcount); 
        rectcount++; 
        rd.setLabel(fent.getName());  
        rd.setModelElement(fent); 
        visuals.add(rd);
      } 
 
      fents.add(fent); 
      mflat.set(ent,fent); 
    } 

    for (int i = 0; i < targets.size(); i++) 
    { Entity ent = (Entity) targets.get(i); 
      VisualData vd = getVisualOf(ent); 
      RectData rd0 = (RectData) vd; 
      Entity fent = ent.makeFlattenedCopy(true,tcdepth); 

      if (rd0 != null) 
      { RectData rd = new RectData(rd0.getx(), rd0.gety() + 400,
                                 getForeground(),componentMode,rectcount); 
        rectcount++; 
        rd.setLabel(fent.getName());  
        rd.setModelElement(fent); 
        visuals.add(rd);
      } 
 
      fents.add(fent); 
      mflat.set(ent,fent); 
    } 

    Vector originalunused = new Vector(); 
    originalunused.addAll(entities); 
    originalunused.removeAll(sources); 
    originalunused.removeAll(targets); 

    for (int i = 0; i < originalunused.size(); i++) 
    { Entity ent = (Entity) originalunused.get(i); 
      VisualData vd = getVisualOf(ent); 
      RectData rd0 = (RectData) vd; 
      Entity fent = ent.makeFlattenedCopy(true,scdepth); 

      if (rd0 != null) 
      { RectData rd = new RectData(rd0.getx(), rd0.gety() + 400,
                                 getForeground(),componentMode,rectcount); 
        rectcount++; 
        rd.setLabel(fent.getName());  
        rd.setModelElement(fent); 
        visuals.add(rd);
      } 
 
      fents.add(fent); 
      mflat.set(ent,fent); 
    } 


    for (int j = 0; j < entities.size(); j++) 
    { Entity ex = (Entity) entities.get(j); 
      Entity fex = (Entity) mflat.get(ex); 
      fex.copyInheritances(ex,mflat); 
    } 

    Vector originalentities = new Vector(); 
    originalentities.addAll(entities); 

    entities.addAll(fents); 

    Vector osources = new Vector(); 
    Vector otargets = new Vector(); 
    Vector concretesources = new Vector(); 
    Vector concretetargets = new Vector(); 

    Vector unused = new Vector(); 
    unused.addAll(fents); 
    
    
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      if (ei.isConcrete()) 
      { concretesources.add(mflat.get(ei)); }
      osources.add(mflat.get(ei));   
    } 

    for (int j = 0; j < targets.size(); j++) 
    { Entity ej = (Entity) targets.get(j); 
      if (ej.isConcrete()) 
      { concretetargets.add(ej); }
      otargets.add(mflat.get(ej));       
    } 

    unused.removeAll(osources); 
    unused.removeAll(otargets);  
    /* These are neither source or target, so are shared and copied */ 

    Vector emapsources = new Vector(); 
    Vector emaptargets = new Vector();
    Vector emapsrcs = new Vector(); 
    Map emap = new Map(); 
 
    for (int i = 0; i < entitymaps.size(); i++) 
    { EntityMatching em = (EntityMatching) entitymaps.get(i); 
      Entity esrc = (Entity) ModelElement.lookupByName(em.realsrc.getName() + "$", entities); 
      Entity etrg = (Entity) ModelElement.lookupByName(em.realtrg.getName() + "$", entities); 
      if (esrc != null && etrg != null) 
      { emapsources.add(esrc); 
        emaptargets.add(etrg); 
        emapsrcs.add(em.realsrc); 
        emap.set(em.realsrc,em.realtrg); 
      } 
    } 
    // osources.removeAll(emapsources); 
    // concretesources.removeAll(emapsources); 

    // Vector allbmaps = Map.allMaps(sources,targets); 
    
    // System.out.println("**** EVALUATING " + allbmaps.size() + " entity mappings"); 

    java.util.Date date1 = new java.util.Date(); 
    long time1 = date1.getTime(); 

    Vector best = new Vector(); 
    Map mm = new Map(); 
    Map mmalternatives = new Map(); 

    double bestscore = 0; 

    for (int i = 0; i < sources.size(); i++) 
    { Entity se = (Entity) sources.get(i); 
      // String sename = se.getName(); 
      double sbest = 0; 
      Entity smatch = null; 
      Vector salternatives = new Vector(); 

      if (emapsrcs.contains(se))
      { smatch = (Entity) emap.get(se);
        salternatives.add(smatch); 
        sbest = 1; 
      }  
      else 
      { for (int j = 0; j < targets.size(); j++) 
        { Entity te = (Entity) targets.get(j); 
          // String tename = te.getName(); 

          Vector ctargets = new Vector(); 
          boolean concrete2abstract = false; 

          double sim = 0; 
          if (se.isConcrete() && te.isAbstract())
          { concrete2abstract = true; } 
          sim = se.nmsSimilarity(te,thesaurus);  
          System.out.println(">>> NMS of " + se + " " + te + " is: " + sim); 
 
          if (sim > sbest) 
          { sbest = sim; 
            // salternatives.clear(); 
            if (concrete2abstract) 
            { ctargets = te.getAllConcreteSubclasses(); 
              for (int h = 0; h < ctargets.size(); h++) 
              { Entity ctarg = (Entity) ctargets.get(h); 
                if (se.nmsSimilarity(ctarg,thesaurus) > 0 && !salternatives.contains(ctarg))
                { salternatives.add(ctarg); } 
              }  
            } 
            else if (salternatives.contains(te)) { } 
            else  
            { salternatives.add(te); }  
            smatch = te; 
          } 
          else if (sim > 0.5 && sim == sbest) 
          { if (concrete2abstract) 
            { ctargets = te.getAllConcreteSubclasses(); 
              for (int h = 0; h < ctargets.size(); h++) 
              { Entity ctarg = (Entity) ctargets.get(h); 
                if (se.nmsSimilarity(ctarg,thesaurus) > 0 && !salternatives.contains(ctarg))
                { salternatives.add(ctarg); } 
              }  
            } 
            else if (salternatives.contains(te)) { } 
            else  
            { salternatives.add(te); }
          }  
        } 
      }

      if (smatch != null) 
      { mm.set(se,smatch); 
        mmalternatives.set(se,salternatives); 
        bestscore = bestscore + sbest; 
      } 
    } // create a set of maps


    System.out.println("===== The best name semantics map is " + mm); 
    System.out.println("===== with name semantic score " + bestscore); 

    Map addedmaps = new Map(); 

    for (int i = 0; i < sources.size(); i++) 
    { Entity ss = (Entity) sources.get(i); 
      Vector ssalt = (Vector) mmalternatives.get(ss); 
      if (ssalt == null) 
      { // if ss is a subclass of x in mmalternatives.domain()
        // then alternatives for ss are mmalternatives.get(x) and their descendents

        Vector desc = mmalternatives.descendents(ss); 
        if (desc.size() > 0)
        { addedmaps.set(ss,desc); } 
        else 
        { Vector asc = mmalternatives.ancestors(ss); 
          if (asc.size() > 0) 
          { addedmaps.set(ss,asc); } 
          else if (ss.isConcrete())
          { addedmaps.set(ss,concretetargets); } 
          else 
          { addedmaps.set(ss,targets); } 
        } 
      } 
    } 
    mmalternatives.elements.addAll(addedmaps.elements); 

    System.out.println("===== Alternative maps are " + mmalternatives); 
    System.out.println();      

    // Vector alternativemaps = Map.submappings(mmalternatives.elements); 
    // but only retain those that respect inheritance. 
    // System.out.println(alternativemaps); 

    Vector allmaps = convertTo$Forms(mmalternatives.elements,mflat); 
    Map alternativesmap = new Map(allmaps); 
    LexMultiOrdering lex = new LexMultiOrdering(alternativesmap); 

    System.out.println("===== Alternative maps are " + alternativesmap); 
    System.out.println("===== Lex: " + lex); 
    Vector msources = alternativesmap.domain(); 
    System.out.println("===== msources: " + msources); 
    int maxsize = msources.size(); 
      

    if (osources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (otargets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 
    
    System.out.println("**** EVALUATING maps from " + osources + " to " + otargets); 


    Map nmsbest = null; // mm; 
    // ModelMatching nmsmodmatch = new ModelMatching(nmsbest,entities); 
    Vector bestmodms = new Vector(); // nmsmodmatch; 

    double nmsbestscore = 0; 
    // ModelMatching.dssSimilarity(nmsbest,nmsmodmatch,entities); 

    // for (int i = 0; i < allmaps.size(); i++) 
    // { Vector altmap = (Vector) allmaps.get(i); 
    Vector word = new Vector(); 
    word = (Vector) lex.increment(maxsize,word);

    if (injectiveOnly == false)  
    { while (word.size() < maxsize) 
      { System.out.println(">>> skipping " + word); 
        word = (Vector) lex.increment(maxsize,word); 
      } 
    } 
    
    while (word.size() <= maxsize)
    { System.out.println(">>> processing " + word); 

      Map newmap = LexMultiOrdering.getMap(word,msources); // new Map(altmap); 
      // Map newmap1 = Map.extendDomainRange(newmap,unused); 
      // Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
      ModelMatching newmodmatch = new ModelMatching(newmap,entities);  
      // double newdd = ModelMatching.dssSimilarity(newmap2,newmodmatch,entities);
      // double newdd = ModelMatching.compositeSimilarity(newmap2,newmodmatch,entities);

      double mapscore = 0; 
      double factor = 1; 

      if (newmodmatch.isMonotonic()) { } 
      else 
      { // System.out.println("non-monotonic map: " + newmap); 
        factor = 0; 
      } 

      if (injectiveOnly && newmap.isInjective()) { } 
      else if (injectiveOnly)
      { // System.out.println("non-injective map: " + newmap); 
        factor = 0; 
      } 

      Vector csources = newmap.domain(); 
      // System.out.println(">>> Processing map: " + newmap); 

      for (int j = 0; j < csources.size(); j++) 
      { Entity ei = (Entity) csources.get(j); 
        Entity ej = (Entity) newmap.get(ei); 
   
        if (ei.isConcrete() && ej.isAbstract()) 
        { factor = 0; } 
        else if (factor > 0)
        { double dd = ei.similarity(ej,newmodmatch,entities,false,false,thesaurus); 
          double namesim = ModelElement.similarity(ei.getName().toLowerCase(),
                                                   ej.getName().toLowerCase()); 
          double nmssim = ei.nms$Similarity(ej,thesaurus); 
          mapscore = mapscore + dd  + namesim*ModelMatching.NAMEWEIGHT + 
                                      nmssim*ModelMatching.NMSWEIGHT; 
        } 
        // System.out.println("Structural similarity of " + ei + " and " + ej + " is: " + dd); 
        // System.out.println();  
      }

      // for (int k = 0; k < unused.size(); k++) 
      // { Entity ux = (Entity) unused.get(k); 

      mapscore = mapscore*factor; 

      if (mapscore > nmsbestscore) // && GAIndividual.isValid(newmap2)) 
      { System.out.println("*** Improved match: " + newmap); 
        nmsbest = newmap; 
        nmsbestscore = mapscore; 
        bestmodms.clear(); 
        bestmodms.add(newmodmatch);  
      } 
      else if (mapscore > 0 && mapscore == nmsbestscore) 
      { if (bestmodms.contains(newmodmatch)) { } 
        else 
        { bestmodms.add(newmodmatch); } 
      } 
      word = (Vector) lex.increment(maxsize,word); 
    } 
    System.out.println("*** Optimised name semantics match is " + nmsbest); 
    System.out.println("*** Optimal name semantics matches are " + bestmodms);

    if (bestmodms.size() == 0) 
    { return; } 

    ModelMatching selected = (ModelMatching) bestmodms.get(0); 
    double snamesim = selected.nameSimilarity(); 
    System.out.println(">>>> Name similarity of " + selected.mymap + " is " + snamesim); 
      
    for (int j = 1; j < bestmodms.size(); j++) 
    { ModelMatching bb = (ModelMatching) bestmodms.get(j); 
      double nsimbb = bb.nameSimilarity(); 
      System.out.println(">>>> Name similarity of " + bb.mymap + " is " + nsimbb); 
      if (bb.size() < selected.size())
      { selected = bb; } 
      else if (nsimbb > snamesim)
      { selected = bb; } 
    } 

    java.util.Date date2 = new java.util.Date(); 
    long time2 = date2.getTime(); 

    System.out.println("===== Execution time = " + (time2 - time1)); 

    System.out.println("===== The selected map is " + selected);
    System.out.println(); 
    synthesiseTransformations(selected,originalentities);  
  } 

  public void iterativeOptimisation(String kind, Vector thesaurus)
  { Vector fents = new Vector(); 
    Map mflat = new Map(); 
    Vector initialPopulation = new Vector(); 

    boolean allmaps = false; 
    boolean strict = false; 

    if ("all maps".equals(kind)) // including superclasses
    { allmaps = true; } 
    else if ("inheritance-preserving".equals(kind))
    { allmaps = true; 
      strict = true; 
    } 
   
    String compositionDepth = JOptionPane.showInputDialog("Max feature chain length? (<= 10, >= 1): ");
    int cdepth = Integer.parseInt(compositionDepth); 
   
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      
      VisualData vd = getVisualOf(ent); 
      RectData rd0 = (RectData) vd; 
      Entity fent = ent.makeFlattenedCopy(allmaps,cdepth); 
      
      if (rd0 != null) 
      { RectData rd = new RectData(rd0.getx(), rd0.gety() + 400,
                                 getForeground(),componentMode,rectcount); 
        rectcount++; 
        rd.setLabel(fent.getName());  
        rd.setModelElement(fent); 
        visuals.add(rd);
      } 
 
      fents.add(fent); 
      mflat.set(ent,fent); 
    } 

    for (int j = 0; j < entities.size(); j++) 
    { Entity ex = (Entity) entities.get(j); 
      Entity fex = (Entity) mflat.get(ex); 
      fex.copyInheritances(ex,mflat); 
    } 

    for (int k = 0; k < fents.size(); k++) 
    { Entity fent = (Entity) fents.get(k); 
      fent.computeLeafs(); 
    } 

    Vector originalentities = new Vector(); 
    originalentities.addAll(entities); 

    entities.addAll(fents); 

    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 
    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Vector oconcretesources = new Vector(); 
    Vector oconcretetargets = new Vector(); 
    Vector osources = new Vector(); 
    Vector otargets = new Vector(); 

    Vector unused = new Vector(); 
    unused.addAll(fents); 
    
    
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      if (ei.getName().endsWith("$"))
      { if (ei.isConcrete()) 
        { oconcretesources.add(ei); }
        osources.add(ei); 
      }  
    } 

    for (int j = 0; j < targets.size(); j++) 
    { Entity ej = (Entity) targets.get(j); 
      if (ej.getName().endsWith("$"))
      { if (ej.isConcrete())
        { oconcretetargets.add(ej); }
        otargets.add(ej); 
      }  
    } 

    unused.removeAll(osources); 
    unused.removeAll(otargets);  
    /* These are neither source or target, so are shared and copied */ 

    // Vector allbmaps; 

    Vector emapsources = new Vector(); 
    Vector emaptargets = new Vector();
 
    for (int i = 0; i < entitymaps.size(); i++) 
    { EntityMatching em = (EntityMatching) entitymaps.get(i); 
      Entity esrc = (Entity) ModelElement.lookupByName(em.realsrc.getName() + "$", entities); 
      Entity etrg = (Entity) ModelElement.lookupByName(em.realtrg.getName() + "$", entities); 
      if (esrc != null && etrg != null) 
      { emapsources.add(esrc); 
        emaptargets.add(etrg); 
      } 
    } 
    osources.removeAll(emapsources); 
    oconcretesources.removeAll(emapsources); 

    if (allmaps) { } 
    else 
    { otargets.removeAll(emaptargets);
      oconcretetargets.removeAll(emaptargets); 
    } // can't be used for other entities

    System.out.println("**** Assumed matchings are: " + emapsources + " -> " + emaptargets); 


    if (otargets.size() <= 1) 
    { // no choice in the map
      Map mm0 = new Map(osources,otargets); 
      Map mm1 = Map.extendDomainRange(mm0,unused); 
      Map mm2 = Map.extendDomainRange(mm1,emapsources,emaptargets); 

      ModelMatching modmatch = new ModelMatching(mm2,entities); 

      Vector csources = mm2.domain(); 
      for (int i = 0; i < csources.size(); i++) 
      { Entity ei = (Entity) csources.get(i); 
        Entity ej = (Entity) mm2.get(ei); 
        
        double dd = ei.similarity(ej,modmatch,entities,false,strict,thesaurus); 
      } 
      System.out.println("===== The only possible map is " + modmatch);
      synthesiseTransformations(modmatch,originalentities); 
      return; 
    } 

    System.out.println("**** EVALUATING maps from " + osources + " to " + otargets); 

    if (osources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (otargets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Map mmname = new Map(); 
    double bestnamescore = 0; 

    for (int i = 0; i < osources.size(); i++) 
    { Entity se = (Entity) osources.get(i); 
      String sename = se.getName(); 
      double sbest = 0; 
      Entity smatch = null; 

      for (int j = 0; j < otargets.size(); j++) 
      { Entity te = (Entity) otargets.get(j); 
        String tename = te.getName(); 
        double sim = ModelElement.similarity(sename.toLowerCase(), tename.toLowerCase()); 
        if (sim > sbest) 
        { sbest = sim; 
          smatch = te; 
        } 
      }
      if (smatch != null) 
      { mmname.set(se,smatch); 
        bestnamescore = bestnamescore + sbest; 
      } 
    } // create a set of maps

    Map name_mm = Map.extendDomainRange(mmname,unused); 
    Map name_mm1 = Map.extendDomainRange(name_mm,emapsources,emaptargets); 

    System.out.println("*** Best name match is " + name_mm1); 
    initialPopulation.add(name_mm1); 

    Map namebest = name_mm1; 
    ModelMatching modmatch = new ModelMatching(namebest,entities); 

    double namebestscore = ModelMatching.dssSimilarity(namebest,modmatch,entities,thesaurus); 

    for (int i = 0; i < mmname.elements.size(); i++) 
    { Maplet nmaplet = (Maplet) mmname.elements.get(i); 
      Entity nsrc = (Entity) nmaplet.source; 
      Entity ntrg = (Entity) namebest.get(nsrc); 
      for (int k = 0; k < otargets.size(); k++) 
      { Entity ktrg = (Entity) otargets.get(k); 
        if (ntrg != null && ntrg != ktrg) 
        { Map newmap = new Map(namebest); 
          newmap.set(nsrc,ktrg); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          ModelMatching newmodmatch = new ModelMatching(newmap2,entities);  
          double newdd = ModelMatching.dssSimilarity(newmap2,newmodmatch,entities,thesaurus);
          if (newdd > namebestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Improved match: " + newmap2); 
            namebest = newmap2; 
            namebestscore = newdd; 
          } 
        } 
      } 
    } 
    System.out.println("*** Optimised name match is " + namebest); 

    Vector optdom = namebest.domain(); 
    for (int i = 0; i < osources.size(); i++) 
    { Entity ss = (Entity) osources.get(i); 
      if (optdom.contains(ss)) { } 
      else 
      { for (int j = 0; j < otargets.size(); j++) 
        { Entity tt = (Entity) otargets.get(j); 
          Map newmap = new Map(namebest); 
          newmap.set(ss,tt); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          // ModelMatching newmodmatch = new ModelMatching(newmap2);  
          double newdd = ModelMatching.compositeSimilarity(newmap2,entities);
          if (newdd > namebestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Added: " + newmap2); 
            namebest = newmap2; 
            namebestscore = newdd; 
          } 
        }           
      }
    } 

    Map mmnms = new Map(); 
    double bestnmsscore = 0; 

    for (int i = 0; i < osources.size(); i++) 
    { Entity se = (Entity) osources.get(i); 
      // String sename = se.getName(); 
      double sbest = 0; 
      Entity smatch = null; 

      for (int j = 0; j < otargets.size(); j++) 
      { Entity te = (Entity) otargets.get(j); 
        // String tename = te.getName(); 
        double sim = se.nms$Similarity(te,thesaurus); 
        if (sim > sbest) 
        { sbest = sim; 
          smatch = te; 
        } 
      }
      if (smatch != null) 
      { mmnms.set(se,smatch); 
        bestnmsscore = bestnmsscore + sbest; 
      } 
    } // create a set of maps

    Map nms_mm = Map.extendDomainRange(mmnms,unused); 
    Map nms_mm1 = Map.extendDomainRange(nms_mm,emapsources,emaptargets); 

    System.out.println("*** Best name semantics match is " + nms_mm1); 
    initialPopulation.add(nms_mm1); 

    Map nmsbest = nms_mm1; 
    ModelMatching nmsmodmatch = new ModelMatching(nmsbest,entities); 

    double nmsbestscore = ModelMatching.dssSimilarity(nmsbest,nmsmodmatch,entities,thesaurus); 

    for (int i = 0; i < nms_mm1.elements.size(); i++) 
    { Maplet nmaplet = (Maplet) nms_mm1.elements.get(i); 
      Entity nsrc = (Entity) nmaplet.source; 
      Entity ntrg = (Entity) nmsbest.get(nsrc); 
      for (int k = 0; k < otargets.size(); k++) 
      { Entity ktrg = (Entity) otargets.get(k); 
        if (ntrg != null && ntrg != ktrg) 
        { Map newmap = new Map(nmsbest); 
          newmap.set(nsrc,ktrg); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          ModelMatching newmodmatch = new ModelMatching(newmap2,entities);  
          double newdd = ModelMatching.dssSimilarity(newmap2,newmodmatch,entities,thesaurus);
          if (newdd > nmsbestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Improved match: " + newmap2); 
            nmsbest = newmap2; 
            nmsbestscore = newdd; 
          } 
        } 
      } 
    } 
    System.out.println("*** Optimised name semantics match is " + nmsbest); 

    Vector optnmsdom = nmsbest.domain(); 
    for (int i = 0; i < osources.size(); i++) 
    { Entity ss = (Entity) osources.get(i); 
      if (optnmsdom.contains(ss)) { } 
      else 
      { for (int j = 0; j < otargets.size(); j++) 
        { Entity tt = (Entity) otargets.get(j); 
          Map newmap = new Map(nmsbest); 
          newmap.set(ss,tt); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          // ModelMatching newmodmatch = new ModelMatching(newmap2);  
          double newdd = ModelMatching.compositeSimilarity(newmap2,entities);
          if (newdd > nmsbestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Added: " + newmap2); 
            nmsbest = newmap2; 
            nmsbestscore = newdd; 
          } 
        }           
      }
    } 

    Map overallbest = nmsbest; 
    if (nmsbestscore < namebestscore)
    { overallbest = namebest; } 

    System.out.println(">>> Overall best map is: " + overallbest); 

    ModelMatching overallmatch = new ModelMatching(overallbest,entities); 

    double overallbestscore = ModelMatching.dssSimilarity(overallbest,overallmatch,entities,thesaurus); 

    for (int i = 0; i < overallbest.elements.size(); i++) 
    { Maplet nmaplet = (Maplet) overallbest.elements.get(i); 
      Entity nsrc = (Entity) nmaplet.source; 
      Entity ntrg = (Entity) overallbest.get(nsrc); 
      for (int k = 0; k < otargets.size(); k++) 
      { Entity ktrg = (Entity) otargets.get(k); 
        if (ntrg != null && ntrg != ktrg) 
        { Map newmap = new Map(overallbest); 
          newmap.set(nsrc,ktrg); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          ModelMatching newmodmatch = new ModelMatching(newmap2,entities);  
          double newdd = ModelMatching.dssSimilarity(newmap2,newmodmatch,entities,thesaurus);
          if (newdd > overallbestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Improved match: " + newmap2); 
            overallbest = newmap2; 
            overallbestscore = newdd; 
          } 
        } 
      } 
    } 
    System.out.println("*** Optimised overall match is " + overallbest); 

    LexicographicOrdering lex = new LexicographicOrdering(); 
    lex.setalphabet(oconcretetargets); 
    lex.init(); 

    int maxmapsize = oconcretesources.size(); 
    int minmapsize = maxmapsize/2;  

    Vector word = new Vector(); 
    word = (Vector) lex.increment(word); 

    Vector best = new Vector(); 
    double bestscore = overallbestscore; 

    while (word.size() < minmapsize) 
    { word = (Vector) lex.increment(word); }  
    // skip non-total maps

    while (word.size() <= maxmapsize) 
    { Map mm = lex.getMap(word,oconcretesources);  

    // for (int mind = 0; mind < allbmaps.size(); mind++) 
    // { Map mm = (Map) allbmaps.get(mind); 

      if (allmaps) { } 
      else
      { while (!mm.isInjective() && word.size() <= maxmapsize) 
        { word = (Vector) lex.increment(word);
          mm = lex.getMap(word,oconcretesources); 
        }
        // if (word.size() == maxmapsize)
        // { System.out.println(mm + " is injective"); }  
      }   // only injective maps if allmaps == false

      if (word.size() > maxmapsize)
      { break; } 

      Map mm1 = Map.extendDomainRange(mm,unused); 
      Map mm2 = Map.extendDomainRange(mm1,emapsources,emaptargets); 
      Map mm3 = Entity.extendMapToAbstractClasses(mm2,osources,oconcretesources); 


      ModelMatching modmatch3 = new ModelMatching(mm3,entities); 

      /* while (!modmatch.isMonotonic() && word.size() == maxmapsize) 
      { word = (Vector) lex.increment(word); 
        mm = lex.getMap(word,osources); 
        mm1 = Map.extendDomainRange(mm,unused);
        modmatch = new ModelMatching(mm1,entities);
      } // skip non-monotone maps
      */ 

      
      double mapscore = 0; 
      double factor = 1; 

      // System.out.println(">>> Unextended map: " + mm2); 
      Vector csources = mm3.domain(); 
      // System.out.println(">>> Processing extended map: " + mm3); 

      for (int i = 0; i < csources.size(); i++) 
      { Entity ei = (Entity) csources.get(i); 
        Entity ej = (Entity) mm3.get(ei); 
   
        if (ei.isConcrete() && ej.isAbstract()) 
        { double namesim = ModelElement.similarity(ei.getName().toLowerCase(), 
                                                   ej.getName().toLowerCase()); 
          double nmssim = ei.nms$Similarity(ej,thesaurus);
          if (namesim > 0.7 || nmssim > 0.7) 
          { // map ei to all concrete subclasses of ej 
            System.out.println("=== very similar classes " + ei + 
                               " " + ej); 
          }  
          factor = 0; 
        } 
        else if (factor > 0) 
        { double dd = ei.similarity(ej,modmatch3,entities,false,strict,thesaurus); 
          double namesim = ModelElement.similarity(ei.getName(), ej.getName()); 
          double nmssim = ei.nms$Similarity(ej,thesaurus); 
          mapscore = mapscore + dd  + namesim*ModelMatching.NAMEWEIGHT + 
                                      nmssim*ModelMatching.NMSWEIGHT; 
        } 
        // System.out.println("Structural similarity of " + ei + " and " + ej + " is: " + dd); 
        // System.out.println();  
      }

      // for (int k = 0; k < unused.size(); k++) 
      // { Entity ux = (Entity) unused.get(k); 

      mapscore = mapscore*factor; 

      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        best = new Vector(); 
        best.add(modmatch3); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm3); 
        System.out.println("=== Structural similarity map score is " + mapscore);  
        System.out.println(); 
      } 
      else if (mapscore > 0 && mapscore == bestscore)
      { best.add(modmatch3); } 

      word = (Vector) lex.increment(word); 
      // System.out.println("NEXT word: " + word); 
    } 

    System.out.println("===== The best map(s) are " + best); 
    System.out.println("===== with score " + bestscore); 
    System.out.println(); 

    if (best.size() == 0) 
    { return; } 

    ModelMatching selected = (ModelMatching) best.get(0); 
    double snamesim = selected.nameSimilarity(); 
    System.out.println(">>>> Name similarity of " + selected.mymap + " is " + snamesim); 
      
    for (int j = 1; j < best.size(); j++) 
    { ModelMatching bb = (ModelMatching) best.get(j); 
      double nsimbb = bb.nameSimilarity(); 
      System.out.println(">>>> Name similarity of " + bb.mymap + " is " + nsimbb); 
      if (bb.size() < selected.size())
      { selected = bb; } 
      else if (nsimbb > snamesim)
      { selected = bb; } 
    } 

    System.out.println("===== The selected map is " + selected);
    System.out.println(); 
    synthesiseTransformations(selected,originalentities); 
  } 


  public void flattenModel(String kind, Vector thesaurus)
  { Vector fents = new Vector(); 
    Map mflat = new Map(); 

    boolean allmaps = false; 
    boolean strict = false; 

    if ("all maps".equals(kind)) // including superclasses
    { allmaps = true; } 
    else if ("inheritance-preserving".equals(kind))
    { allmaps = true; 
      strict = true; 
    } 

    String compositionDepth = JOptionPane.showInputDialog("Max feature chain length? (<= 10, >= 1): ");
    int cdepth = Integer.parseInt(compositionDepth); 
   
    for (int i = 0; i < entities.size(); i++) 
    { Entity ent = (Entity) entities.get(i); 
      VisualData vd = getVisualOf(ent); 
      RectData rd0 = (RectData) vd; 
      Entity fent = ent.makeFlattenedCopy(allmaps,cdepth); 

      if (rd0 != null) 
      { RectData rd = new RectData(rd0.getx(), rd0.gety() + 400,
                                 getForeground(),componentMode,rectcount); 
        rectcount++; 
        rd.setLabel(fent.getName());  
        rd.setModelElement(fent); 
        visuals.add(rd);
      } 
 
      fents.add(fent); 
      mflat.set(ent,fent); 
    } 

    for (int j = 0; j < entities.size(); j++) 
    { Entity ex = (Entity) entities.get(j); 
      Entity fex = (Entity) mflat.get(ex); 
      fex.copyInheritances(ex,mflat); 
    } 

    Vector originalentities = new Vector(); 
    originalentities.addAll(entities); 

    entities.addAll(fents); 

    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 
    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Vector concretesources = new Vector(); 
    // Vector concretetargets = new Vector(); 
    Vector osources = new Vector(); 
    Vector otargets = new Vector(); 

    Vector unused = new Vector(); 
    unused.addAll(fents); 
    
    
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      if (ei.getName().endsWith("$"))
      { if (ei.isConcrete()) 
        { concretesources.add(ei); }
        osources.add(ei); 
      }  
    } 

    for (int j = 0; j < targets.size(); j++) 
    { Entity ej = (Entity) targets.get(j); 
      if (ej.getName().endsWith("$"))
      { // if (ej.isConcrete())
        // { concretetargets.add(ej); }
        otargets.add(ej); 
      }  
    } 

    unused.removeAll(osources); 
    unused.removeAll(otargets);  
    /* These are neither source or target, so are shared and copied */ 

    // Vector allbmaps; 

    Vector emapsources = new Vector(); 
    Vector emaptargets = new Vector();
 
    for (int i = 0; i < entitymaps.size(); i++) 
    { EntityMatching em = (EntityMatching) entitymaps.get(i); 
      Entity esrc = (Entity) ModelElement.lookupByName(em.realsrc.getName() + "$", entities); 
      Entity etrg = (Entity) ModelElement.lookupByName(em.realtrg.getName() + "$", entities); 
      if (esrc != null && etrg != null) 
      { emapsources.add(esrc); 
        emaptargets.add(etrg); 
      } 
    } 
    osources.removeAll(emapsources); 
    concretesources.removeAll(emapsources); 

    if (allmaps) { } 
    else 
    { otargets.removeAll(emaptargets); } // can't be used for other entities

    System.out.println("**** Assumed matchings are: " + emapsources + " -> " + emaptargets); 

    java.util.Date date1 = new java.util.Date(); 
    long time1 = date1.getTime(); 

    if (otargets.size() <= 1) 
    { // no choice in the map
      Map mm0 = new Map(osources,otargets); 
      Map mm1 = Map.extendDomainRange(mm0,unused); 
      Map mm2 = Map.extendDomainRange(mm1,emapsources,emaptargets); 

      ModelMatching modmatch = new ModelMatching(mm2,entities); 

      Vector csources = mm2.domain(); 
      for (int i = 0; i < csources.size(); i++) 
      { Entity ei = (Entity) csources.get(i); 
        Entity ej = (Entity) mm2.get(ei); 
        
        double dd = ei.similarity(ej,modmatch,entities,false,strict,thesaurus); 
      } 
      System.out.println("===== The only possible map is " + modmatch);
      synthesiseTransformations(modmatch,originalentities); 
      return; 
    } 

    System.out.println("**** EVALUATING maps from " + osources + " to " + otargets); 

    if (osources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (otargets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    LexicographicOrdering lex = new LexicographicOrdering(); 
    lex.setalphabet(otargets); 
    lex.init(); 

    int maxmapsize = osources.size(); 
    int minmapsize = maxmapsize/2; // concretesources.size(); 

    // Vector sortedsources = Entity.orderByInheritance(osources); 

    Vector word = new Vector(); 
    word = (Vector) lex.increment(word); 


    // System.out.println("**** EVALUATING " + allbmaps.size() + " entity mappings"); 
    System.out.println("**** Unused entities are: " + unused); 
    // should include in otargets if not injective. 


    Vector best = new Vector(); 
    double bestscore = 0; 

    while (word.size() < minmapsize) 
    { word = (Vector) lex.increment(word); }  
    // skip non-total maps

    while (word.size() <= maxmapsize) 
    { Map mm = lex.getMap(word,osources);  

    // for (int mind = 0; mind < allbmaps.size(); mind++) 
    // { Map mm = (Map) allbmaps.get(mind); 

      if (allmaps) { } 
      else
      { while (!mm.isInjective() && word.size() <= maxmapsize) 
        { word = (Vector) lex.increment(word);
          mm = lex.getMap(word,osources); 
        }
        // if (word.size() == maxmapsize)
        // { System.out.println(mm + " is injective"); }  
      }   // only injective maps if allmaps == false

      if (word.size() > maxmapsize)
      { break; } 

      Map mm1 = Map.extendDomainRange(mm,unused); 
      Map mm2 = Map.extendDomainRange(mm1,emapsources,emaptargets); 

      ModelMatching modmatch = new ModelMatching(mm2,entities); 

      /* while (!modmatch.isMonotonic() && word.size() == maxmapsize) 
      { word = (Vector) lex.increment(word); 
        mm = lex.getMap(word,osources); 
        mm1 = Map.extendDomainRange(mm,unused);
        modmatch = new ModelMatching(mm1,entities);
      } // skip non-monotone maps
      */ 

      
      double mapscore = 0; 
      double factor = 1; 

      Vector csources = mm2.domain(); 
      // System.out.println(">>> Processing map: " + csources + " |--> " + word); 

      for (int i = 0; i < csources.size(); i++) 
      { Entity ei = (Entity) csources.get(i); 
        Entity ej = (Entity) mm2.get(ei); 
   
        if (ei.isConcrete() && ej.isAbstract()) 
        { factor = 0; } 
        else if (factor > 0) 
        { double dd = ei.similarity(ej,modmatch,entities,false,strict,thesaurus); 
          double namesim = ModelElement.similarity(ei.getName().toLowerCase(),
                                                   ej.getName().toLowerCase()); 
          double nmssim = ei.nms$Similarity(ej,thesaurus); 
          mapscore = mapscore + dd  + namesim*ModelMatching.NAMEWEIGHT + 
                                      nmssim*ModelMatching.NMSWEIGHT; 
        } 
        // System.out.println("Structural similarity of " + ei + " and " + ej + " is: " + dd); 
        // System.out.println();  
      }

      // for (int k = 0; k < unused.size(); k++) 
      // { Entity ux = (Entity) unused.get(k); 

      mapscore = mapscore*factor; 

      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        best = new Vector(); 
        best.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm2); 
        System.out.println("=== Structural similarity map score is " + mapscore);  
        System.out.println(); 
      } 
      else if (mapscore > 0 && mapscore == bestscore)
      { best.add(modmatch); } 

      word = (Vector) lex.increment(word); 
      // System.out.println("NEXT word: " + word); 
    } 

    System.out.println("===== The best map(s) are " + best); 
    System.out.println("===== with score " + bestscore); 
    System.out.println(); 

    // Take the smallest one? 

    if (best.size() == 0) 
    { return; } 

    ModelMatching selected = (ModelMatching) best.get(0); 
    double snamesim = selected.nameSimilarity(); 
    System.out.println(">>>> Name similarity of " + selected.mymap + " is " + snamesim); 
      
    for (int j = 1; j < best.size(); j++) 
    { ModelMatching bb = (ModelMatching) best.get(j); 
      double nsimbb = bb.nameSimilarity(); 
      System.out.println(">>>> Name similarity of " + bb.mymap + " is " + nsimbb); 
      if (bb.size() < selected.size())
      { selected = bb; } 
      else if (nsimbb > snamesim)
      { selected = bb; } 
    } 

    java.util.Date date2 = new java.util.Date(); 
    long time2 = date2.getTime(); 
    System.out.println(">>> Execution time = " + (time2 - time1)); 

    System.out.println("===== The selected map is " + selected);
    System.out.println(); 
    synthesiseTransformations(selected,originalentities); 

  } 

  public void flattenModelGA(Vector thesaurus)
  { Vector initialPopulation = new Vector(); 
    GeneticAlgorithm ga = new GeneticAlgorithm(50); 

    Vector sources = getSourceEntities(); 
    Vector targets = getTargetEntities(); 

    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    Vector concretesources = new Vector(); 
    Vector originalentities = new Vector(); 
    originalentities.addAll(entities); 

    String compositionDepth = JOptionPane.showInputDialog("Max feature chain length? (1 or 2): ");
    int cdepth = Integer.parseInt(compositionDepth); 
    
    for (int i = 0; i < entities.size(); i++) 
    { Entity ei = (Entity) entities.get(i); 
      ei.defineLocalFeatures(); 
      if (cdepth > 1) 
      { ei.defineNonLocalFeatures(); }  
    } 
    
    for (int i = 0; i < sources.size(); i++) 
    { Entity ei = (Entity) sources.get(i); 
      if (ei.isConcrete()) 
      { concretesources.add(ei); } 
    } 
      

    Vector unused = new Vector(); 
    unused.addAll(entities); 
    unused.removeAll(sources); 
    unused.removeAll(targets); 

    Vector emapsources = new Vector(); 
    Vector emaptargets = new Vector();
 
    for (int i = 0; i < entitymaps.size(); i++) 
    { EntityMatching em = (EntityMatching) entitymaps.get(i); 
      emapsources.add(em.realsrc); 
      emaptargets.add(em.realtrg);  
    } 
    sources.removeAll(emapsources); 
    concretesources.removeAll(emapsources); 

    // Vector allbmaps = Map.allMaps(sources,targets); 

    if (sources.size() == 0) 
    { System.err.println("!!! ERROR: please define some source-stereotyped entities"); 
      return; 
    } 
    if (targets.size() == 0) 
    { System.err.println("!!! ERROR: please define some target-stereotyped entities"); 
      return; 
    } 

    String initialPop = JOptionPane.showInputDialog("Initial population size (<= 100, >= 10): ");
    int inipop = Integer.parseInt(initialPop); 
    String iterationsMax = JOptionPane.showInputDialog("Number of iterations? (<= 100, >= 10): ");
    int iters = Integer.parseInt(iterationsMax); 

    
    System.out.println("**** EVALUATING maps from " + sources + " to " + targets); 

    Map mmname = new Map(); 
    double bestnamescore = 0; 

    for (int i = 0; i < sources.size(); i++) 
    { Entity se = (Entity) sources.get(i); 
      String sename = se.getName(); 
      double sbest = 0; 
      Entity smatch = null; 

      for (int j = 0; j < targets.size(); j++) 
      { Entity te = (Entity) targets.get(j); 
        String tename = te.getName(); 
        double sim = ModelElement.similarity(sename.toLowerCase(), tename.toLowerCase()); 
        if (sim > sbest) 
        { sbest = sim; 
          smatch = te; 
        } 
      }
      if (smatch != null) 
      { mmname.set(se,smatch); 
        bestnamescore = bestnamescore + sbest; 
      } 
    } // create a set of maps

    Map name_mm = Map.extendDomainRange(mmname,unused); 
    Map name_mm1 = Map.extendDomainRange(name_mm,emapsources,emaptargets); 

    System.out.println("*** Best name match is " + name_mm1); 
    initialPopulation.add(name_mm1); 

    Map namebest = name_mm1; 
    double namebestscore = ModelMatching.compositeSimilarity(namebest,entities); 

    for (int i = 0; i < mmname.elements.size(); i++) 
    { Maplet nmaplet = (Maplet) mmname.elements.get(i); 
      Entity nsrc = (Entity) nmaplet.source; 
      Entity ntrg = (Entity) namebest.get(nsrc); 
      for (int k = 0; k < targets.size(); k++) 
      { Entity ktrg = (Entity) targets.get(k); 
        if (ntrg != null && ntrg != ktrg) 
        { Map newmap = new Map(namebest); 
          newmap.set(nsrc,ktrg); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          // ModelMatching newmodmatch = new ModelMatching(newmap2);  
          double newdd = ModelMatching.compositeSimilarity(newmap2,entities);
          if (newdd > namebestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Added: " + newmap2); 
            namebest = newmap2; 
            namebestscore = newdd; 
          } 
        } 
      } 
    } 
    System.out.println("*** Optimised name match is " + namebest); 

    Vector optdom = namebest.domain(); 
    for (int i = 0; i < sources.size(); i++) 
    { Entity ss = (Entity) sources.get(i); 
      if (optdom.contains(ss)) { } 
      else 
      { for (int j = 0; j < targets.size(); j++) 
        { Entity tt = (Entity) targets.get(j); 
          Map newmap = new Map(namebest); 
          newmap.set(ss,tt); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          // ModelMatching newmodmatch = new ModelMatching(newmap2);  
          double newdd = ModelMatching.compositeSimilarity(newmap2,entities);
          if (newdd > namebestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Added: " + newmap2); 
            namebest = newmap2; 
            namebestscore = newdd; 
          } 
        }           
      }
    } 

    Map mmnms = new Map(); 
    double bestnmsscore = 0; 

    for (int i = 0; i < sources.size(); i++) 
    { Entity se = (Entity) sources.get(i); 
      // String sename = se.getName(); 
      double sbest = 0; 
      Entity smatch = null; 

      for (int j = 0; j < targets.size(); j++) 
      { Entity te = (Entity) targets.get(j); 
        // String tename = te.getName(); 
        double sim = se.nmsSimilarity(te,thesaurus); 
        if (sim > sbest) 
        { sbest = sim; 
          smatch = te; 
        } 
      }
      if (smatch != null) 
      { mmnms.set(se,smatch); 
        bestnmsscore = bestnmsscore + sbest; 
      } 
    } // create a set of maps

    Map nms_mm = Map.extendDomainRange(mmnms,unused); 
    Map nms_mm1 = Map.extendDomainRange(nms_mm,emapsources,emaptargets); 

    System.out.println("*** Best name semantics match is " + nms_mm1); 
    initialPopulation.add(nms_mm1); 

    Map nmsbest = nms_mm1; 
    ModelMatching nmsmodmatch = new ModelMatching(nmsbest,entities); 

    double nmsbestscore = ModelMatching.dssSimilarity(nmsbest,nmsmodmatch,entities,thesaurus); 

    for (int i = 0; i < nms_mm1.elements.size(); i++) 
    { Maplet nmaplet = (Maplet) nms_mm1.elements.get(i); 
      Entity nsrc = (Entity) nmaplet.source; 
      Entity ntrg = (Entity) nmsbest.get(nsrc); 
      for (int k = 0; k < targets.size(); k++) 
      { Entity ktrg = (Entity) targets.get(k); 
        if (ntrg != null && ntrg != ktrg) 
        { Map newmap = new Map(nmsbest); 
          newmap.set(nsrc,ktrg); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          ModelMatching newmodmatch = new ModelMatching(newmap2,entities);  
          double newdd = ModelMatching.dssSimilarity(newmap2,newmodmatch,entities,thesaurus);
          if (newdd > nmsbestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Improved match: " + newmap2); 
            nmsbest = newmap2; 
            nmsbestscore = newdd; 
          } 
        } 
      } 
    } 
    System.out.println("*** Optimised name semantics match is " + nmsbest); 

    Vector optnmsdom = nmsbest.domain(); 
    for (int i = 0; i < sources.size(); i++) 
    { Entity ss = (Entity) sources.get(i); 
      if (optnmsdom.contains(ss)) { } 
      else 
      { for (int j = 0; j < targets.size(); j++) 
        { Entity tt = (Entity) targets.get(j); 
          Map newmap = new Map(nmsbest); 
          newmap.set(ss,tt); 
          Map newmap1 = Map.extendDomainRange(newmap,unused); 
          Map newmap2 = Map.extendDomainRange(newmap1,emapsources,emaptargets);
          // ModelMatching newmodmatch = new ModelMatching(newmap2);  
          double newdd = ModelMatching.compositeSimilarity(newmap2,entities);
          if (newdd > nmsbestscore && GAIndividual.isValid(newmap2)) 
          { initialPopulation.add(newmap2); 
            System.out.println("*** Added: " + newmap2); 
            nmsbest = newmap2; 
            nmsbestscore = newdd; 
          } 
        }           
      }
    } 



    
    double bestscore = 0; 
    Vector best = new Vector(); 
    double bestabscore = 0; 
    Vector bestab = new Vector(); 
    double bestbxscore = 0; 
    Vector bestbx = new Vector(); 

    LexicographicOrdering lex = new LexicographicOrdering(); 
    lex.setalphabet(targets); 
    lex.init(); 

    int maxmapsize = sources.size(); 
    int minmapsize = maxmapsize/2; // targets.size(); 


    Vector word = new Vector(); 
    word = (Vector) lex.increment(word); 

    while (word.size() < minmapsize) 
    { word = (Vector) lex.increment(word); }  
    // skip non-total maps

    // for (int mind = 0; mind < allbmaps.size(); mind++)
    while (word.size() <= maxmapsize) 
    { // Map mm0 = (Map) allbmaps.get(mind); 
      Map mm0 = lex.getMap(word,sources);  
      Map mm = Map.extendDomainRange(mm0,unused); 
      Map mm1 = Map.extendDomainRange(mm,emapsources,emaptargets); 
      ModelMatching modmatch = new ModelMatching(mm1);
      
      double mapscore = 0; 
      double mapabscore = 0; 
      double mapbxscore = 0; 
      double factor = 1; 

      Vector emaps = mm1.getElements(); 
      for (int i = 0; i < emaps.size(); i++)
      { Maplet emap = (Maplet) emaps.get(i); 
        Entity src = (Entity) emap.source; 
        Entity trg = (Entity) emap.dest; 

        /* if (src.isConcrete() && trg.isAbstract()) 
        { Vector trgsubs = new Vector(); 
          trgsubs.addAll(trg.getAllConcreteSubclasses()); 
          // update mm1 with each src --> trgsub and evaluate
          // Map mm2 = new Map(); 
          // mm2.elements.addAll(mm1.elements); 
          for (int j = 0; j < trgsubs.size(); j++) 
          { Entity trgsub = (Entity) trgsubs.get(j); 
            mm1.elements.add(new Maplet(src,trgsub)); 
          } 
          for (int j = 0; j < trgsubs.size(); j++) 
          { Entity trgsub = (Entity) trgsubs.get(j); 
            double rscore = src.esim(trgsub,mm1,modmatch,entities); 
            mapscore = mapscore + rscore; 
            double ascore = src.esimAbs(trgsub,mm1,modmatch,entities); 
            mapabscore += ascore; 
            mapbxscore += rscore*ascore; 
          } 
          // mm1 = mm2; 
        } 
        else */  
        if (src.isConcrete() && trg.isAbstract()) 
        { factor = 0; } // skip this map
        else if (factor > 0) 
        { double refscore = src.esim(trg,mm1,modmatch,entities);
          mapscore = mapscore + refscore; 
          double abscore = src.esimAbs(trg,mm1,modmatch,entities);
          mapabscore = mapabscore + abscore; 
          mapbxscore = mapbxscore + refscore*abscore; 
        } 
      } 

      mapscore = mapscore*factor; 
      mapabscore = mapabscore*factor; 
      mapbxscore = mapbxscore*factor; 
      
      if (mapscore > bestscore) 
      { bestscore = mapscore; 
        best = new Vector(); 
        best.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Refinement map score is " + mapscore);  
        initialPopulation.add(mm1); 
        System.out.println(); 
      } 
      else if (mapscore > 0 && mapscore == bestscore)
      { best.add(modmatch); } 

      if (mapabscore > bestabscore) 
      { bestabscore = mapabscore; 
        bestab = new Vector(); 
        bestab.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Abstraction map score is " + mapabscore);  
        initialPopulation.add(mm1); 
        System.out.println(); 
      } 
      else if (mapabscore > 0 && mapabscore == bestabscore)
      { bestab.add(modmatch); } 

      if (mapbxscore > bestbxscore) 
      { bestbxscore = mapbxscore; 
        bestbx = new Vector(); 
        bestbx.add(modmatch); 
        System.out.println(); 
        System.out.println("========= For entity mapping " + mm1); 
        System.out.println("=== Bx map score is " + mapbxscore);  
        initialPopulation.add(mm1); 
        System.out.println(); 
      } 
      else if (mapbxscore > 0 && mapbxscore == bestbxscore)
      { bestbx.add(modmatch); } 


      if (initialPopulation.size() >= inipop) 
      { break; } 

      word = (Vector) lex.increment(word); 
    } 

    if (initialPopulation.size() == 0) 
    { return; } 

    ga.initialise(initialPopulation); 
    ga.iterate(iters,entities,sources,targets,unused,emapsources,emaptargets,thesaurus); 

    best = ga.getBest(); 
    if (best.size() == 0) 
    { return; } 

    ModelMatching selected = (ModelMatching) best.get(0); 
    double snamesim = selected.nameSimilarity(); 
    System.out.println("==== Name similarity of " + selected.mymap + " is " + snamesim); 
      
    for (int j = 1; j < best.size(); j++) 
    { ModelMatching bb = (ModelMatching) best.get(j); 
      double nsimbb = bb.nameSimilarity(); 
      System.out.println("==== Name similarity of " + bb.mymap + " is " + nsimbb); 
      if (bb.size() < selected.size())
      { selected = bb; } 
      else if (nsimbb > snamesim)
      { selected = bb; } 
    } 

    System.out.println("===== The selected map is " + selected);
    System.out.println(); 

    if (selected != null)
    { Vector csources = selected.mymap.domain(); 
      for (int i = 0; i < csources.size(); i++) 
      { Entity ei = (Entity) csources.get(i); 
        Entity ej = (Entity) selected.mymap.get(ei); 
        
        double dd = ei.compositeSimilarity(ej,selected.mymap,selected,entities);
      } 
      synthesiseTransformations(selected,entities); 
    }
  } 


  private void synthesiseTransformations(ModelMatching selected, Vector originalentities)
  { selected.removeInvalidMatchings(); 
    selected.copySuperclassMatchings(); 
    selected.checkBidirectionalAssociationConsistency(); 
    selected.checkEntityMapCompleteness(originalentities); 

    Vector corrpatts = selected.analyseCorrelationPatterns(originalentities); 
    System.out.println("----------Correlation patterns are: ----------------------------"); 
    System.out.println();
    for (int y = 0; y < corrpatts.size(); y++) 
    { CorrelationPattern cp = (CorrelationPattern) corrpatts.get(y);
      System.out.println(cp); 
      System.out.println(); 
    }   

    System.out.println("----------------------------------------------------------------"); 

    try
    { PrintWriter cout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter("output/forward.txt")));

      cout.println("----------------------- Forward map is \n" + selected); 

      cout.println("/* QVT-R transformation: */"); 

      cout.println(selected.qvtTransformation(types)); 

      for (int i = 0; i < types.size(); i++) 
      { Type et = (Type) types.get(i); 
        if (et.isEnumeration())
        { cout.println(et.enumQueryOps()); } 
      }     

      cout.println("/* UML-RSDS transformation: */"); 

      cout.println(selected.umlrsdsTransformation()); 

      cout.println("/* QVT-O transformation: */"); 

      cout.println(selected.qvtoTransformation(types)); 

      cout.println("/* ATL transformation: */"); 

      cout.println(selected.atlTransformation(types)); 

      cout.close();
    }
    catch (Exception ex) 
    { ex.printStackTrace(); }


    try
    { PrintWriter rout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter("output/reverse.txt")));
      ModelMatching inv = selected.invert(); 
      rout.println("----------------------- Reverse map is \n" + inv); 

      rout.println("/* Reverse QVT-R transformation: */"); 

      rout.println(inv.qvtTransformation(types)); 

      for (int i = 0; i < types.size(); i++) 
      { Type et = (Type) types.get(i); 
        if (et.isEnumeration())
        { rout.println(et.enumQueryOps()); } 
      }     

      rout.println("/* Reverse UML-RSDS transformation: */"); 

      rout.println(inv.umlrsdsTransformation()); 

      rout.println("/* Reverse QVT-O transformation: */"); 

      rout.println(inv.qvtoTransformation(types)); 

      rout.println("/* Reverse ATL transformation: */"); 

      rout.println(inv.atlTransformation(types)); 

      rout.close();
    }
    catch (Exception ex) 
    { ex.printStackTrace(); }
  } 

  public void testCases()
  { Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      Vector tests = e.testCases(); 
      System.out.println("*** Test cases for entity " + e.getName() + " are: " + tests); 
      for (int j = 0; j < tests.size(); j++) 
      { String tst = (String) tests.get(j); 
        try
        { PrintWriter rout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter("output/test" + e.getName() + "_" + j + ".txt")));
          rout.println(tst); 
          rout.close(); 
        } 
        catch (Exception _x) { } 
      } 
    } 
  } 
} 


class PreUseCase
{ String nme; 
  String ent; 
  String role; 

  PreUseCase(String name, String e, String rle)
  { nme = name; 
    ent = e; 
    role = rle; 
  } 
} 



class TextFileFilter extends javax.swing.filechooser.FileFilter
{ public boolean accept(File f) 
  { if (f.isDirectory()) { return true; } 

    if (f.getName().endsWith(".txt")) { return true; } 

    return false; 
  } 

  public String getDescription()
  { return "Select a .txt file"; } 
}

class KM3FileFilter extends javax.swing.filechooser.FileFilter
{ public boolean accept(File f) 
  { if (f.isDirectory()) { return true; } 

    if (f.getName().endsWith(".km3")) { return true; } 

    return false; 
  } 

  public String getDescription()
  { return "Select a .km3 file"; } 
}

class ATLFileFilter extends javax.swing.filechooser.FileFilter
{ public boolean accept(File f) 
  { if (f.isDirectory()) { return true; } 

    if (f.getName().endsWith(".atl")) { return true; } 

    return false; 
  } 

  public String getDescription()
  { return "Select a .atl file"; } 
}

class ETLFileFilter extends javax.swing.filechooser.FileFilter
{ public boolean accept(File f) 
  { if (f.isDirectory()) { return true; } 

    if (f.getName().endsWith(".etl")) { return true; } 

    return false; 
  } 

  public String getDescription()
  { return "Select an .etl file"; } 
}
