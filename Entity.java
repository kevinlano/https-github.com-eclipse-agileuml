import java.util.Vector; 
import java.util.List;
import java.util.HashSet;  
import java.io.*; 
import javax.swing.*;

/******************************
* Copyright (c) 2003,2019 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/
/* package: Class Diagram       */ 

public class Entity extends ModelElement implements Comparable
{ private Vector attributes = new Vector();   // Attribute
  private Vector associations = new Vector(); // Association
    // The associations in which this is entity1
  private Vector operations = new Vector();   // BehaviouralFeature
  private Entity superclass = null; 
  private Vector superclasses = null; // in case of C++ only 
  private Vector subclasses = new Vector();  // Entity
  private Vector invariants = new Vector();  // Constraint
  private Vector interfaces = new Vector();  // Interface classes
  private String cardinality = null; // describes number of instances allowed
  private Association linkedAssociation = null; // for association class only
  private Statemachine behaviour = null; 
  private Statement activity = null; 
  private String auxiliaryElements = ""; 
  private String privateAux = ""; // for C++ only 

  private Vector localFeatures = new Vector(); 
  private Vector nonlocalFeatures = new Vector(); 
  private Vector allLeafSubclasses = new Vector(); 

  
  public Entity(String nme)
  { super(nme); }

  public boolean notEmpty()
  { if (attributes.size() > 0) { return true; } 
    if (operations.size() > 0) { return true; } 
    return false; 
  } // or associations 

  public int featureCount()
  { return attributes.size() + associations.size(); } 

  public void setStatechart(Statemachine sc)
  { behaviour = sc; } 

  public Statemachine getStatechart()
  { return behaviour; } 

  public Vector getLocalFeatures()
  { return localFeatures; } 

  public Vector getNonLocalFeatures()
  { return nonlocalFeatures; } 

  public Vector getLocalBooleanFeatures()
  { Vector res = new Vector(); 

    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      Type t = att.getType(); 
      if (t != null && t.getName().equals("boolean"))
      { res.add(att); } 
    } 

    if (res.size() > 0) 
    { return res; } 

    for (int i = 0; i < localFeatures.size(); i++) 
    { Attribute att = (Attribute) localFeatures.get(i); 
      Type t = att.getType(); 
      if (t != null && t.getName().equals("boolean"))
      { res.add(att); } 
    } 

    return res; 
  } 

  public Vector getLocalReferenceFeatures()
  { Vector res = new Vector(); 

    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      { res.add(new Attribute(ast)); } 
    } 

    if (res.size() > 0) 
    { return res; } 

    for (int i = 0; i < localFeatures.size(); i++) 
    { Attribute att = (Attribute) localFeatures.get(i); 
      Type t = att.getType(); 
      if (t != null && t.isEntity())
      { res.add(att); } 
    } 

    return res; 
  } 

  public Vector getNonLocalTargetFeatures(Map mm)
  { // only include r.s where intermediate entity is not in mm.range, & is concrete
    Vector rang = mm.range(); 

    Vector res = new Vector(); 
    for (int i = 0; i < nonlocalFeatures.size(); i++) 
    { Attribute att = (Attribute) nonlocalFeatures.get(i); 
      Entity e1 = att.intermediateEntity(); 
      if (e1 != null && !(rang.contains(e1)) && e1.isConcrete())
      { res.add(att); } 
    } 
    return res; 
  } 

  public void defineLocalFeatures()
  { localFeatures.clear(); 
    
    Vector atts = allDefinedAssociations(); 
    Vector batts = allDefinedAttributes(); 

    localFeatures.addAll(batts); 
    for (int i = 0; i < atts.size(); i++) 
    { Association ast = (Association) atts.get(i); 
      Attribute datt = new Attribute(ast); 
      localFeatures.add(datt); 
    } 
  } 

  public void defineNonLocalFeatures()
  { nonlocalFeatures.clear(); 
    Vector asts = allDefinedAssociations(); 
    for (int i = 0; i < asts.size(); i++) 
    { Association ast = (Association) asts.get(i); 
      Attribute step1 = new Attribute(ast); 

      Entity e2 = ast.getEntity2(); 
      Vector v1 = e2.allDefinedAttributes(); 
      Vector v2 = e2.allDefinedAssociations(); 
      for (int j = 0; j < v1.size(); j++) 
      { Attribute step2 = (Attribute) v1.get(j); 

        Vector path = new Vector();        
        path.add(step1); path.add(step2); 
        Attribute newatt = new Attribute(path); 
        nonlocalFeatures.add(newatt); 
      } 

      for (int k = 0; k < v2.size(); k++) 
      { Association ast2 = (Association) v2.get(k);
        Attribute step2 = new Attribute(ast2);  
        if (step1.equals(step2)) { } 
        else if (step1.isForbiddenInverse(step2)) { } 
        else 
        { Vector path = new Vector(); 
          path.add(step1); path.add(step2); 
          Attribute newatt = new Attribute(path); 
          nonlocalFeatures.add(newatt); 
        } 
      } 
    }       
  } 

  public void computeLeafs()
  { allLeafSubclasses.clear(); 
    allLeafSubclasses.addAll(getActualLeafSubclasses()); 
  } 

  public Entity makeFlattenedCopy(boolean allmaps, int n)
  { // Combine all direct attributes, associations and inherited and
    // indirect data features. Up to maximum length of chain = n

    Entity res = new Entity(getName() + "$"); 

    Vector atts = allDefinedAttributes(); 

    for (int i = 0; i < atts.size(); i++) 
    { Attribute att = (Attribute) atts.get(i); 
      Attribute newatt = (Attribute) att.clone(); 
      // newatt.setEntity(res);
      res.addAttribute(newatt); 
      newatt.setEntity(this);  // ie., its original owner, not res. 
    }     

    Vector asts = allDefinedAssociations(); 

    for (int i = 0; i < asts.size(); i++) 
    { Association ast = (Association) asts.get(i); 
      if (isSourceEntity()) 
      { if (ast.isTarget()) { continue; } } 
      else if (isTargetEntity())
      { if (ast.isSource()) { continue; } }  
             
      Attribute newatt = new Attribute(ast);
      Vector astlist = new Vector(); 
      astlist.add(newatt);  
      newatt.setNavigation(astlist); 
      // newatt.setEntity(res);
      res.addAttribute(newatt); 
      newatt.setEntity(this);  
    }     


    HashSet seen = new HashSet(); 
    seen.add(this); 
    Vector comps; 
    if (allmaps) 
    { comps = composedProperties1(seen,n); } 
    else 
    { comps = composedProperties(seen,n); } 
 
    // System.out.println("Composed properties of " + getName() + " are: " + comps); 
    for (int j = 0; j < comps.size(); j++) 
    { Vector props = (Vector) comps.get(j); 
      if (props.size() < 1) { }  
      else if (props.size() == 1) 
      { Attribute atn = (Attribute) props.get(0); 
        if (res.hasAttribute(atn.getName())) { } 
        else 
        { Attribute newatt = new Attribute(props); 
          res.addAttribute(newatt); 
          newatt.setEntity(this);  
        }
      } 
      else  
      { Attribute att = new Attribute(props); 
        // System.out.println("NEW attribute " + att.getName() + " : " + att.getType());  
        if (res.hasAttribute(att.getName())) { } 
        else
        { res.addAttribute(att);
          att.setEntity(this);  
        }
      }  
    } 

    res.behaviour = behaviour;  // clone it

    Vector strs = getStereotypes(); 
    for (int j = 0; j < strs.size(); j++) 
    { String stereo = (String) strs.get(j); 
      res.addStereotype(stereo);  
    }  
    res.activity = activity;
    return res; 
  } 

  public void copyInheritances(Entity original, Map mflat)
  { if (original.superclass != null) 
    { Entity scopy = (Entity) mflat.get(original.superclass); 
      superclass = scopy; 
    } 

    if (superclasses != null) 
    { for (int i = 0; i < original.superclasses.size(); i++) 
      { Entity sub = (Entity) original.superclasses.get(i); 
        Entity fsub = (Entity) mflat.get(sub); 
        superclasses.add(fsub); 
      }
    } 

    for (int i = 0; i < original.subclasses.size(); i++) 
    { Entity sub = (Entity) original.subclasses.get(i); 
      Entity fsub = (Entity) mflat.get(sub); 
      subclasses.add(fsub); 
    }
  } 

  public static Vector orderByInheritance(Vector entities)
  { // places a superclass before its subclasses
    Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      if (res.contains(e)) { } 
      else 
      { res.add(e); 
        res.addAll(e.getAllSubclasses()); 
      } 
    } 
    return res; 
  } 

  public Entity targetCopy()
  { Entity res = new Entity("OUT$" + getName()); 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      Attribute newatt = (Attribute) att.clone(); 
      newatt.setEntity(res); 
      res.addAttribute(newatt); 
    }     
    res.behaviour = behaviour;  // clone it
    res.addStereotype("target");
    // Vector strs = getStereotypes(); 
    for (int j = 0; j < stereotypes.size(); j++) 
    { String stereo = (String) stereotypes.get(j); 
      if ("source".equals(stereo)) { } 
      else 
      { res.addStereotype(stereo); }  
    }  
    res.activity = activity;
    return res; 
  } 

  public Vector copyToTarget(Entity trg, java.util.Map srctrg)
  { Vector res = new Vector(); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      Association newast = (Association) ast.clone(); 
      newast.setEntity1(trg); 
      Entity e2 = ast.getEntity2(); 
      Entity newe2 = (Entity) srctrg.get(e2);
      newast.setEntity2(newe2);
      trg.addAssociation(newast);   
      res.add(newast); 
    }     

    if (superclass != null) 
    { Entity newsup = (Entity) srctrg.get(superclass); 
      trg.setSuperclass(newsup);
      newsup.addSubclass(trg);  
      Generalisation gen = new Generalisation(newsup,trg); 
      res.add(gen); 
    } 
    return res; 
  } // also copy operations?

  public Association getLinkedAssociation()
  { return linkedAssociation; } 

  public Statement getActivity()
  { return activity; } 

  public void setActivity(Statement s) 
  { activity = s; } 

  public void addPrimaryKey(String nme) 
  { Attribute att = new Attribute(nme,new Type("String",null), ModelElement.INTERNAL); 
    att.setUnique(true); 
    att.setEntity(this); 
    attributes.add(0,att); 
  } 
 
  public void addAttribute(Attribute att)
  { if (att == null || attributes.contains(att)) { return; } 

    Attribute oldatt = getAttribute(att.getName()); 
    if (oldatt != null) 
    { attributes.remove(oldatt); } 
    else 
    { Attribute supatt = getDefinedAttribute(att.getName()); 
      if (supatt != null) 
      { System.err.println("WARNING: " + att + " is already declared in a superclass.");  
        // return; 
      }
    } 
      

    if (isInterface())
    { if (att.isClassScope() && att.isFrozen()) { } 
      else 
      { System.err.println("DECLARATION ERROR: Only frozen class-scope attributes " +
                           "allowed in interfaces!"); 
        // return; 
      }
      att.setVisibility(PUBLIC);  
    }

    if (isAbstract()) 
    { att.setVisibility(PROTECTED); } 

    attributes.add(att);
    att.setEntity(this);  // Usually this is correct, but sometimes we need to override this
  }

  public void addAttributes(List atts)
  { for (int i = 0; i < atts.size(); i++) 
    { Attribute att = (Attribute) atts.get(i); 
      addAttribute(att); 
    }
  }

  public void removeAttribute(String nme)
  { Vector removals = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.getName().equals(nme))
      { removals.add(att); }
    }
    attributes.removeAll(removals); 
  }  // will need to recheck invariants, type-check

  public void removeAtts(List atts) 
  { for (int i = 0; i < atts.size(); i++) 
    { Attribute att = (Attribute) atts.get(i); 
      removeAttribute(att.getName()); 
    } 
  }

  public void pushdownAttributes() // for interfaces only
  { if (isInterface()) {} else { return; } 

    for (int j = 0; j < attributes.size(); j++) 
    { Attribute att = (Attribute) attributes.get(j); 
      if (att.isClassScope() && att.isFrozen()) { } 
      else 
      { for (int i = 0; i < subclasses.size(); i++) 
        { Entity ee = (Entity) subclasses.get(i); 
          ee.addAttribute(att); 
        } 
      } 
    }
    // and remove all attributes?  
  }  

  public void pushdownAssociations(UCDArea ucdArea) // for interfaces only
  { if (isInterface()) {} else { return; } 

    Vector newasts = new Vector(); 

    for (int j = 0; j < associations.size(); j++) 
    { Association ast = (Association) associations.get(j); 
      for (int i = 0; i < subclasses.size(); i++) 
      { Entity ee = (Entity) subclasses.get(i);
        Association newast = new Association(ee,ast.getEntity2(),
                                             ast.getCard1(),ast.getCard2(),
                                             ast.getRole1(),ast.getRole2());  
        newast.setOrdered(ast.isOrdered()); 
        newast.setSorted(ast.isSorted()); 
        // and other propertes
        newast.setAddOnly(ast.isAddOnly());
        newast.setFrozen(ast.isFrozen());
        newast.setAggregation(ast.getAggregation()); 
        newast.setQualifier(ast.getQualifier()); 
        newast.setInstanceScope(ast.getInstanceScope());
        newast.setSource(ast.isSource()); 
        newast.setTarget(ast.isTarget()); 
        ee.addAssociation(newast); 
        newasts.add(newast); 
      }  
    }
    ucdArea.addAssociations(newasts); 
  }  

  public void clearAux()
  { auxiliaryElements = ""; 
    privateAux = ""; 
  } 

  public void addAux(String aux)
  { auxiliaryElements = auxiliaryElements + aux; } 

  public void addPrivateAux(String aux)
  { privateAux = privateAux + aux; } 

  public void removeConstraint(Constraint c)
  { invariants.remove(c); } 

  public void setAttributes(Vector atts)
  { attributes = atts; } 

  public void setOperations(Vector ops) 
  { operations = ops; } 

  public Vector getOperations()
  { return operations; } 


  public int operationsCount() 
  { return operations.size(); } 

  public void addOperation(BehaviouralFeature f)
  { // if (isInterface())
    // { if (f.isAbstract()) { } 
    //  else 
    //  { System.err.println("All interface operations must be abstract"); 
    //    return; 
    //  }
    // }

    // System.out.println("ADDING OPERATION " + f + " TO " + this); 

    String sig = f.getSignature(); 
    Vector removals = new Vector(); 

    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(i); 
      if (sig.equals(bf.getSignature()))  // replace bf by f
      { removals.add(bf); } 
    } 

    // System.out.println("REMOVING OPERATIONS " + removals); 

    operations.removeAll(removals); 

    f.setEntity(this);
    operations.add(f);

    if (isInterface())
    { f.addStereotype("abstract"); } 
  }  // If f is abstract, this must also be

  public void removeOperation(BehaviouralFeature f)
  { operations.remove(f); }  // may invalidate an activity or call of this

  public void removeDerivedOperations()
  { Vector removed = new Vector(); 
    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(i); 
      if (bf.isDerived())
      { removed.add(bf); } 
    } 
    operations.removeAll(removed); 
  } 

  public void removeOperations(String nme)
  { Vector removed = new Vector(); 
    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(i); 
      if (bf.getName().equals(nme))
      { removed.add(bf); } 
    } 
    operations.removeAll(removed); 
  } 

  public void typeCheckOps(Vector types, Vector entities)
  { for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(i); 
      bf.typeCheck(types,entities); 
    } 
  } 

  public void addInterface(Entity intf)
  { // Check that all ops of intf are also in this
    Vector iops = intf.getOperations(); 
    Vector allops = allOperations(); 
    for (int i = 0; i < iops.size(); i++) 
    { BehaviouralFeature bf = (BehaviouralFeature) iops.get(i); 
      boolean found = false; 
      String sig = bf.getSignature();
      for (int j = 0; j < allops.size(); j++) 
      { BehaviouralFeature ebf = (BehaviouralFeature) allops.get(j); 
        String esig = ebf.getSignature(); 
        if (sig.equals(esig))
        { found = true; } 
      } 
      if (found) { } 
      else 
      { System.err.println("!!!!!! Operation " + sig + " of interface " + intf.getName() + 
                           " is not implemented in class " + getName()); 
      }  
    } 
    interfaces.add(intf);  
  }
       
  public void removeInterface(Entity e)
  { interfaces.remove(e); } 

  public Vector getInterfaces()
  { return interfaces; } 

  public void setCardinality(String card)
  { cardinality = card; }  // must be *, 0..n or n for n : NAT

  public String getCardinality()
  { return cardinality; } 

  public boolean isInterface()
  { return hasStereotype("interface"); } 

  public boolean isAbstract()
  { return hasStereotype("abstract"); } 

  public boolean isConcrete()
  { return !hasStereotype("abstract"); } 

  public boolean isActive()
  { return hasStereotype("active"); }

  public boolean isSingleton()
  { return hasStereotype("singleton"); }

  public boolean isSequential()
  { return hasStereotype("sequential"); } 

  public boolean isSourceEntity()
  { return hasStereotype("source"); } 

  public boolean isTargetEntity()
  { return hasStereotype("target"); } 

  public boolean isSharedEntity()
  { if (hasStereotype("source"))
    { return false; } 
    if (hasStereotype("target"))
    { return false; } 
    return true; 
  } 

  public boolean hasNoSourceFeature()
  { boolean res = true; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isSource()) { return false; } 
    } 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      if (ast.isSource())
      { return false; } 
    } 
    return res; 
  } 


  public boolean isSource()
  { return hasStereotype("source"); } 

  public boolean isTarget()
  { return hasStereotype("target"); } 

  public boolean isShared()
  { if (hasStereotype("source"))
    { return false; } 
    if (hasStereotype("target"))
    { return false; } 
    return true; 
  } 
 

  public boolean isExternal()
  { return hasStereotype("external"); } 

  public boolean isExternalApp()
  { return hasStereotype("externalApp"); } 

  public boolean isAssociationClass()
  { return (linkedAssociation != null); } 

  public void setAbstract(boolean b)
  { if (b == false)
    { removeStereotype("abstract"); } 
    else 
    { if (isAbstract()) { } 
      else 
      { addStereotype("abstract"); } 
    } 
  }

  public void setSingleton(boolean b)
  { if (b == false)
    { removeStereotype("singleton"); } 
    else 
    { if (isSingleton()) { } 
      else 
      { addStereotype("singleton"); } 
    } 
  }

  public void setLeaf(boolean b)
  { if (b == false)
    { removeStereotype("leaf"); } 
    else 
    { if (isLeaf()) { } 
      else 
      { addStereotype("leaf"); } 
    } 
  }

  public boolean isRoot()
  { return superclass == null && (superclasses == null || superclasses.size() == 0); } 

  public boolean isLeaf()
  { return hasStereotype("leaf"); } 

  public boolean isActualLeaf()
  { return subclasses.size() == 0; } 
  
  public Vector levelOrder(Vector ents)
  { if (ents.contains(this)) { return ents; } 
    ents.add(this); 
    for (int i = 0; i < subclasses.size(); i++) 
    { Entity sub = (Entity) subclasses.get(i); 
      int j = ents.indexOf(sub); 
      if (j >= 0) 
      { int k0 = ents.indexOf(this);
        if (k0 < j) { } // Is already ok, otherwise move it before sub:  
        else 
        { ents.remove(this); 
          int k = ents.indexOf(sub); 
          ents.add(k,this);
        }  
      } 
      else 
      { sub.levelOrder(ents); }  
    } 
    return ents; 
  } 

  public boolean uniqueConstraint(String att)
  { String nme = getName(); 
    for (int i = 0; i < invariants.size(); i++) 
    { Constraint con = (Constraint) invariants.get(i); 
      Expression succ = con.succedent(); 
      // System.out.println("======> " + succ); 

      if ((succ + "").equals(nme + "->isUnique(" + att + ")"))
      { return true; } 
    } 
    return false; 
  } 

  public Vector getAttributes()
  { return attributes; }

  public Vector allAttributes()
  { Vector res = new Vector(); 
    res.addAll(attributes); 
    res.addAll(allInheritedAttributes()); 
    return res; 
  } 

  public Vector allProperties()
  { Vector res = new Vector();
    Vector assocs = allDataFeatures();  
    for (int i = 0; i < assocs.size(); i++) 
    { Object ast = assocs.get(i); 
      if (ast instanceof Association) 
      { res.add(new Attribute((Association) ast)); } 
      else if (ast instanceof Attribute) 
      { res.add((Attribute) ast); }   
    } 
    // System.out.println("=== All properties of " + getName() + " are " + res); 
    return res; 
  } 

  public Vector composedProperties(HashSet seen, int n)
  { // pre: seen.contains(this)
    Vector res = new Vector();

    if (n <= 0) { return res; } 

    Vector atts = allDefinedAttributes(); 
    for (int a = 0; a < atts.size(); a++)
    { Attribute att = (Attribute) atts.get(a);
      Vector aseq = new Vector();
      aseq.add(att); 
      res.add(aseq);
    }

    Vector asts = allDefinedAssociations(); 
    Vector linkedProperties = new Vector();
    for (int x = 0; x < asts.size(); x++)
    { Association ast = (Association) asts.get(x);
      Entity e2 = ast.getEntity2();
      if (seen.contains(e2)) {}
      else 
      if (isSource() && ast.isTarget()) {} 
      else if (isTarget() && ast.isSource()) {}
      else
      { Attribute r2 = new Attribute(ast);
        // if (seen.contains(e2)) { } 
        // else 
        { linkedProperties.add(r2); } 
        Vector aseq = new Vector();
        aseq.add(r2); 
        res.add(aseq);
      }
    }
 
    for (int k = 0; k < linkedProperties.size(); k++)
    { Attribute p = (Attribute) linkedProperties.get(k);
      Entity pent = p.getElementType().getEntity();

      HashSet seen1 = new HashSet();
      seen1.addAll(seen);

      if (pent != null) 
      { seen1.add(pent); } 

      Vector pseq = pent.composedProperties(seen1, n-1);
      for (int t = 0; t < pseq.size(); t++)
      { Vector sq = (Vector) pseq.get(t);
        Vector nsq = new Vector();
        nsq.add(p); 
        if (sq.size() > 0) 
        { nsq.addAll(sq);
          res.add(nsq);  
        } 
      }
    }
    return res;
  } // but don't add r1.r2 or r2.r1 for one association. 

  public Vector composedProperties1(HashSet seen, int n)
  { // pre: seen.contains(this)
    Vector res = new Vector();

    if (n <= 0) { return res; } 

    Vector atts = allDefinedAttributes(); 
    for (int a = 0; a < atts.size(); a++)
    { Attribute att = (Attribute) atts.get(a);
      Vector aseq = new Vector();
      aseq.add(att); 
      res.add(aseq);
    }

    Vector asts = allDefinedAssociations(); 
    Vector linkedProperties = new Vector();
    for (int x = 0; x < asts.size(); x++)
    { Association ast = (Association) asts.get(x);
      Entity e2 = ast.getEntity2();
      // if (seen.contains(e2)) {}
      // else 
      if (isSource() && ast.isTarget()) {} 
      else if (isTarget() && ast.isSource()) {}
      else
      { Attribute r2 = new Attribute(ast);
        if (seen.contains(e2)) { } 
        else 
        { linkedProperties.add(r2); } 
        Vector aseq = new Vector();
        aseq.add(r2); 
        res.add(aseq);
      }
    }
 
    for (int k = 0; k < linkedProperties.size(); k++)
    { Attribute p = (Attribute) linkedProperties.get(k);
      Entity pent = p.getElementType().getEntity();

      HashSet seen1 = new HashSet();
      seen1.addAll(seen);

      if (pent != null) 
      { seen1.add(pent); } 

      Vector pseq = pent.composedProperties1(seen1, n-1);
      for (int t = 0; t < pseq.size(); t++)
      { Vector sq = (Vector) pseq.get(t);
        Vector nsq = new Vector();
        nsq.add(p); 
        if (sq.size() > 0) 
        { Attribute p1 = (Attribute) sq.get(0); 
          if (sq.contains(p)) { } 
          else if (p.isForbiddenInverse(p1)) { } 
          else 
          { nsq.addAll(sq);
            res.add(nsq);
          } 
        } 
      }
    }
    return res;
  } // but don't add r1.r2 or r2.r1 for one association. 

  public int vertexDegree() 
  { int supc = 0; 
    if (superclass != null) 
    { supc = 1; } 
    supc = supc + associations.size(); 
    supc = supc + subclasses.size(); 
    return supc; 
  } // ignoring superclasses and interfaces for the present. 

  public Vector neighbourhood() 
  { Vector res = new Vector();  

    if (superclass != null) 
    { res.add(superclass); } 

    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      Entity e2 = ast.getEntity2(); 
      if (res.contains(e2)) { } 
      else 
      { res.add(e2); } 
    }  

    for (int j = 0; j < subclasses.size(); j++) 
    { Entity sub = (Entity) subclasses.get(j); 
      if (res.contains(sub)) { } 
      else 
      { res.add(sub); } 
    }  
    return res; 
  } // ignoring superclasses and interfaces for the present. 

  public Map reachableSubgraph()
  { Vector seen = new Vector(); 
    seen.add(this); 
    Map graph = new Map(); 
    graph.set(this,new Integer(0)); 
    return reachableSubgraph(seen,0,graph); 
  } 

  public Map reachableSubgraph(Vector seen, int distance, Map graph) 
  { // e -> n where n is number of steps from self to e
    
    if (superclass != null && !seen.contains(superclass)) 
    { seen.add(superclass); 
      graph.set(superclass, new Integer(distance + 1)); 
      superclass.reachableSubgraph(seen, distance+1, graph); 
    } 

    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      Entity e2 = ast.getEntity2(); 
      if (seen.contains(e2)) { } 
      else 
      { seen.add(e2); 
        graph.set(e2, new Integer(distance+1)); 
        e2.reachableSubgraph(seen,distance+1,graph); 
      } 
    }  

    return graph; 
  } // ignoring subclasses and interfaces for the present. 

  public double disjointEdgeCount(Vector edges) 
  { int totalEdges = 0; 
    int count = 0; 

    Vector nbs = neighbourhood(); 
    totalEdges = nbs.size(); 
    for (int j = 0; j < nbs.size(); j++) 
    { Entity e1 = (Entity) nbs.get(j); 
      Vector nbsj = e1.neighbourhood(); 
      totalEdges = totalEdges + nbsj.size();          
    }         

    for (int i = 0; i < edges.size(); i++) 
    { Maplet edge = (Maplet) edges.get(i); 
      if (edge.source == this || nbs.contains(edge.source))
      { count++; } 
      else if (edge.dest == this || nbs.contains(edge.dest))
      { count++; } 
    } 

    return (1.0*count)/totalEdges; 
  } 

  public boolean isConnectedTo(Entity e) 
  { // e is a direct sub or superclass, or at opposite end of an association

    if (superclass != null && superclass == e) 
    { return true; } 
    // if (e.superclass != null && this == e.superclass) 
    // { return true; }

    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      if (e == ast.getEntity2())
      { return true; } 
    } 
    return false; 
  } 
 

  public double bonding() 
  { Vector nbs = neighbourhood(); 
    int total = 0; 
    double connections = 0.0; 
    for (int i = 0; i < nbs.size(); i++) 
    { Entity n1 = (Entity) nbs.get(i); 
      for (int j = 0; j < nbs.size(); j++) 
      { Entity n2 = (Entity) nbs.get(j); 
        if (n1 != n2)
        { total++; 
          if (n1.isConnectedTo(n2))
          { connections++; } 
        } 
      } 
    } 

    if (total == 0) 
    { return 1.0; } 
    return connections/total; 
  } 

  public double similarity(Entity ent) 
  { // matchedproperties.size() / max(properties.size(), ent.properties.size())

    Vector eatts = ent.getAttributes(); 
    int ssize = attributes.size(); 
    int esize = eatts.size(); 

    // System.out.println("Class " + getName() + " has attributes " + attributes); 

    if (ssize == 0 && esize == 0) 
    { return 1.0; } 

    Vector matched = new Vector(); 
    Vector ematched = new Vector(); 
  
    for (int i = 0; i < ssize; i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attname = att.getName(); 

      Type t = att.getType(); 
      String tstr = "" + t; 
      double bestsim = 0;
      Attribute best = null; 
  
      for (int j = 0; j < eatts.size(); j++) 
      { Attribute eatt = (Attribute) eatts.get(j); 
        String eattname = eatt.getName(); 

        if (ematched.contains(eatt)) { } 
        else 
        { String estr = eatt.getType() + ""; 

          System.out.println(">>> Comparing " + att + " : " + tstr + " and " + eatt + " : " + estr); 

          if (tstr.equals(estr))
          { double sim = ModelElement.similarity(attname,eattname); 
            if (sim > bestsim) 
            { best = eatt; 
              bestsim = sim; 
            } 
          }
        } 
      } 

      if (bestsim > 0) 
      { matched.add(att); 
        ematched.add(best); 
      } 
    } 

    System.out.println("Matching for " + getName() + " and " + 
                        ent.getName() + " is: "); 
    System.out.println(matched + " -to- " + ematched);
    System.out.println(); 
     
    if (matched.size() == 0) 
    { return 0; } 
    int m = Math.max(ssize,esize); 
    return (1.0*matched.size())/m; 
  } 


  public Vector getNonSourceAttributes()
  { Vector res = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isSource()) { } 
      else 
      { res.add(att); } 
    } 
    return res; 
  } 

  public Vector getNonTargetAttributes()
  { Vector res = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isTarget()) { } 
      else 
      { res.add(att); } 
    } 
    return res; 
  } 

  public static Vector getDirectAttributes(Vector atts)
  { Vector res = new Vector(); 
    for (int i = 0; i < atts.size(); i++) 
    { Attribute att = (Attribute) atts.get(i); 
      if (att.isDirect())
      { res.add(att); } 
    } 
    return res; 
  } 

  public static Vector getComposedAttributes(Vector atts)
  { Vector res = new Vector(); 
    for (int i = 0; i < atts.size(); i++) 
    { Attribute att = (Attribute) atts.get(i); 
      if (att.isDirect()) { } 
      else 
      { res.add(att); } 
    } 
    return res; 
  } 

  public double similarity(Entity ent, ModelMatching modm, Vector entities, 
                           boolean unidirectional,
                           boolean strict, Vector thesaurus) 
  { // find exactly matched properties first, then consider others
    // for matches of the same score, take the one with highest name similarity

    // if (isShared()) { just look for matches for "source" features }
    // if (ent.isShared()) { just use its "target" features & basic attributes } 
    // same situation as this == ent


    Vector satts = getNonTargetAttributes(); 
    Vector eatts = ent.getNonSourceAttributes(); 
    int ssize = satts.size(); 
    int esize = eatts.size(); 
    double score = 0; 
    Map mm = modm.mymap; 

    // System.out.println("Class " + getName() + " has attributes " + attributes); 

    if (ssize == 0 && esize == 0) 
    { return 1.0; } 
    else if (esize == 0) 
    { return 0; } 

    Vector matched = new Vector(); 
    Vector ematched = new Vector(); 
    Vector ignored = new Vector(); 

    if (strict && superclass != null) 
    { // if modm aleady has superclass mapping, use its attribute mappings. 
      Entity smatch = (Entity) mm.get(superclass); 
      if (smatch != null) 
      { if (ent == smatch || isAncestor(smatch,ent)) { } 
        else 
        { // System.out.println(">>> cannot match " + getName() + " to " + 
          //                    ent.getName() + " : not descendent of superclass target " + 
          //                    smatch.getName()); 
          return 0; 
        } 

        EntityMatching em = modm.getMatching(superclass); 
        if (em != null) 
        { Vector amaps = em.getAttributeMatchings(); 

          if (amaps != null) 
          { for (int i = 0; i < amaps.size(); i++) 
            { AttributeMatching am = (AttributeMatching) amaps.get(i); 
              Attribute srcatt = (Attribute) ModelElement.lookupByName(am.src.getName(),satts); 
              Attribute trgatt = (Attribute) ModelElement.lookupByName(am.trg.getName(),eatts); 
              if (srcatt != null && trgatt != null) 
              { matched.add(srcatt); 
                ematched.add(trgatt); 
                score++; 
                // System.out.println(">>> Copied match " + srcatt + " >>> " + trgatt + 
                //                " from " + superclass + " to " + this); 
              } 
            } 
          }
        }
        // else 
        // { System.err.println(">>> Cannot match " + superclass + " in " + modm); }   
      } 
    } 
    else if (strict && subclasses.size() > 0) 
    { for (int i = 0; i < subclasses.size(); i++) 
      { Entity sub = (Entity) subclasses.get(i); 
        Entity submatch = (Entity) mm.get(sub); 
        if (submatch != null) 
        { if (ent == submatch || Entity.isAncestor(ent,submatch)) { } 
          else 
          { // System.out.println(">>> cannot match " + getName() + " to " + 
            //                  ent.getName() + " : not ancestor of subclass target " + 
            //                  submatch.getName()); 
            return 0; 
          } 
        } 
      } // copy attribute matches to the sub - or done in correlation pattern analysis 
    } 


    Vector datts = new Vector(); 
    datts.addAll(Entity.getDirectAttributes(satts)); 
    Vector tatts = new Vector(); 
    tatts.addAll(Entity.getDirectAttributes(eatts)); 
    datts.removeAll(matched); 
    tatts.removeAll(ematched); 
    int dsize = datts.size(); 

    /* Exact matches of local source to local targets with same name */ 

    for (int i = 0; i < dsize; i++) 
    { Attribute att = (Attribute) datts.get(i); 
      String attname = att.getName(); 
      Type t = att.getType(); 
      String sstr = "" + t; 

      if (isShared() && !att.isSource())
      { ignored.add(att); 
        continue; 
      } 
      else if (t.isEntity()) 
      { Entity srcent = t.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc);
           
          // look for trgent without the $
          String trgentname = trgent + ""; 
          sstr = trgentname.substring(0,trgentname.length()-1);
          // System.out.println("LOOKING FOR " + sstr);  
        } 
        // else ignore this attribute for matching and similarity
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 
      else if (Type.isEntityCollection(t))
      { Type elemt = t.getElementType(); 
        Entity srcent = elemt.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc); 
          String trgentname = trgent + ""; 
          sstr = t.getName() + "(" + trgentname.substring(0,trgentname.length()-1) + ")";
          // System.out.println("LOOKING FOR " + sstr);  
          // look for Coll(trgent)
        }
        // else ignore att 
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 

      score = score + att.findExactTypeMatchSameName(tatts,sstr,matched,ematched);       
    } 

    datts.removeAll(matched); 
    datts.removeAll(ignored); 
    dsize = datts.size(); 
    tatts.removeAll(ematched); 

    /* Exact matches of local source to local target */ 

    for (int i = 0; i < dsize; i++) 
    { Attribute att = (Attribute) datts.get(i); 
      String attname = att.getName(); 
      Type t = att.getType(); 
      String sstr = "" + t; 

      if (isShared() && !att.isSource())
      { ignored.add(att); 
        continue; 
      } 
      else if (t.isEntity()) 
      { Entity srcent = t.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc);
           
          // look for trgent without the $
          String trgentname = trgent + ""; 
          sstr = trgentname.substring(0,trgentname.length()-1);
          // System.out.println("LOOKING FOR " + sstr);  
        } 
        // else ignore this attribute for matching and similarity
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 
      else if (Type.isEntityCollection(t))
      { Type elemt = t.getElementType(); 
        Entity srcent = elemt.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc); 
          String trgentname = trgent + ""; 
          sstr = t.getName() + "(" + trgentname.substring(0,trgentname.length()-1) + ")";
          // System.out.println("LOOKING FOR " + sstr);  
          // look for Coll(trgent)
        }
        // else ignore att 
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 

      score = score + att.findExactTypeMatch(tatts,sstr,matched,ematched,thesaurus);       
    } 

    datts.removeAll(matched); 
    datts.removeAll(ignored); 
    dsize = datts.size(); 
    tatts.removeAll(ematched); 

    Vector ctatts = new Vector(); 
    ctatts.addAll(Entity.getComposedAttributes(eatts)); 

    /* Exact matches of local source to composed target */ 

    for (int i = 0; i < dsize; i++) 
    { Attribute att = (Attribute) datts.get(i); 
      String attname = att.getName(); 
      Type t = att.getType(); 
      String sstr = "" + t; 

      if (isShared() && !att.isSource())
      { ignored.add(att); 
        continue; 
      } 
      else if (t.isEntity()) 
      { Entity srcent = t.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc);
           
          // look for trgent without the $
          String trgentname = trgent + ""; 
          sstr = trgentname.substring(0,trgentname.length()-1);
          // System.out.println("LOOKING FOR " + sstr);  
        } 
        // else ignore this attribute for matching and similarity
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 
      else if (Type.isEntityCollection(t))
      { Type elemt = t.getElementType(); 
        Entity srcent = elemt.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc); 
          String trgentname = trgent + ""; 
          sstr = t.getName() + "(" + trgentname.substring(0,trgentname.length()-1) + ")";
          // System.out.println("LOOKING FOR " + sstr);  
          // look for Coll(trgent)
        }
        // else ignore att 
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 

      score = score + att.findExactTypeMatch(ctatts,sstr,matched,ematched,thesaurus);       
    } 

    datts.removeAll(matched); 
    datts.removeAll(ignored); 
    dsize = datts.size(); 
    tatts.removeAll(ematched); 
    ctatts.removeAll(ematched); 

    // Now find the partial matches of local sources to local targets

    for (int i = 0; i < dsize; i++) 
    { Attribute att = (Attribute) datts.get(i); 
      String attname = att.getName(); 

      if (matched.contains(att)) { continue; } 
      if (isShared() && !att.isSource()) { continue; } 

      Type elemt = att.getElementType(); 

      Entity srcent = null; 
      if (elemt != null) 
      { srcent = elemt.getEntity(); } 
 
      if (srcent != null) 
      { String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc); 
          String trgentname = trgent + ""; 
          String testring = trgentname.substring(0,trgentname.length()-1);  
          score = score + 
                    att.findPartialTypeMatch(tatts,testring,mm,entities,
                                             matched,ematched,thesaurus);           
        }
      }  
      else // basic types
      { score = score + att.findPartialBasicMatch(tatts,mm,entities,
                                                  matched,ematched,thesaurus); }  
    } 

    Vector catts = new Vector(); 
    catts.addAll(Entity.getComposedAttributes(satts)); 
    int csize = catts.size(); 
    tatts.removeAll(ematched); 
      
    /* Exact matches of non-local sources to local targets */ 

    for (int i = 0; i < csize; i++) 
    { Attribute att = (Attribute) catts.get(i); 
      String attname = att.getName(); 
      Type t = att.getType(); 
      String sstr = "" + t; 

      if (isShared() && !att.isSource())
      { ignored.add(att); 
        continue; 
      } 
      else if (t.isEntity()) 
      { Entity srcent = t.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc);
           
          // look for trgent without the $
          String trgentname = trgent + ""; 
          sstr = trgentname.substring(0,trgentname.length()-1);
          // System.out.println("LOOKING FOR " + sstr);  
        } 
        // else ignore this attribute for matching and similarity
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 
      else if (Type.isEntityCollection(t))
      { Type elemt = t.getElementType(); 
        Entity srcent = elemt.getEntity(); 
        String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc); 
          String trgentname = trgent + ""; 
          sstr = t.getName() + "(" + trgentname.substring(0,trgentname.length()-1) + ")";
          // System.out.println("LOOKING FOR " + sstr);  
          // look for Coll(trgent)
        }
        // else ignore att 
        else 
        { ignored.add(att); 
          continue; 
        } 
      } 

      score = score + att.findExactTypeMatch(tatts,sstr,matched,ematched,thesaurus);       
    } 

    catts.removeAll(matched); 
    tatts.removeAll(ematched); 
    csize = catts.size(); 

    // Now find the partial matches of non-local sources to local targets

    for (int i = 0; i < csize; i++) 
    { Attribute att = (Attribute) catts.get(i); 
      String attname = att.getName(); 

      if (matched.contains(att)) { continue; } 
      if (isShared() && !att.isSource()) { continue; } 

      Type elemt = att.getElementType(); 

      Entity srcent = null; 
      if (elemt != null) 
      { srcent = elemt.getEntity(); } 
 
      if (srcent != null) 
      { String name$ = srcent.getName() + "$"; 
        Entity fsrc = (Entity) ModelElement.lookupByName(name$, entities); 
        if (fsrc != null && mm.get(fsrc) != null)
        { Entity trgent = (Entity) mm.get(fsrc); 
          String trgentname = trgent + ""; 
          String testring = trgentname.substring(0,trgentname.length()-1);  
          score = score + 
                    att.findPartialTypeMatch(tatts,testring,mm,entities,
                                             matched,ematched,thesaurus);           
        }
      }  
      else // basic types
      { score = score + att.findPartialBasicMatch(tatts,mm,entities,
                                                  matched,ematched,thesaurus);
      }  
    } 

    /* System.out.println("Matching for " + getName() + " and " + 
                        ent.getName() + " is: ");
    System.out.println(matched + " -to- " + ematched);
    System.out.println(); */  

    modm.setAttributeMatches(this,ent,matched,ematched); 
     
    if (matched.size() == 0) 
    { return 0; } 
    int ig = ignored.size(); 
    int m = Math.max(ssize-ig,esize);
    if (unidirectional) 
    { m = ssize - ig; 
      if (m <= 0) 
      { return 1; } 
    } 
    // and ignore any ent attributes whose entity type/element type is not in mm.range() 

    return score/m; 
  } 
  // another combinator, for unidirectional, is score/(ssize - ig)

  public Vector allAttributeNames()
  { Vector res = allAttributes(); 
    return ModelElement.getNames(res); 
  } 

  public Vector allStringAttributeNames()
  { Vector res = allAttributes(); 
    Vector satts = new Vector(); 
    for (int i = 0; i < res.size(); i++) 
    { Attribute att = (Attribute) res.get(i); 
      Type t = att.getType(); 
      if ("String".equals(t.getName()))
      { satts.add(att); }
    }  
    return ModelElement.getNames(satts); 
  } 

  public Vector getUniqueAttributes()
  { Vector res = new Vector(); 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isUnique()) // att is a primary key 
      { res.add(att); } 
    } 
    return res; 
  } 

  public Vector allDefinedUniqueAttributes()
  { Vector res = getUniqueAttributes(); 
    if (superclass == null) 
    { return res; } 
    else 
    { res.addAll(superclass.allDefinedUniqueAttributes()); } 
    return res; 
  } 

  public Attribute getUniqueAttribute(String nme)
  { Attribute res = getDefinedAttribute(nme); 
    if (res != null && res.isUnique()) // att is a primary key 
    { return res; } 
    return null;  
  } 


  public boolean isStaticFeature(String f)
  { Association ast = getDefinedRole(f); 
    if (ast != null) 
    { return ast.isClassScope(); } 
    Attribute att = getDefinedAttribute(f); 
    if (att != null) 
    { return att.isClassScope(); }
    BehaviouralFeature op = getDefinedOperation(f); 
    if (op != null) 
    { return op.isStatic(); } 
    return false; 
  } 

  public Vector getAssociations()
  { return associations; } 

  public Vector allAssociations()
  { Vector res = new Vector(); 
    res.addAll(associations); 
    res.addAll(allInheritedAssociations()); 
    return res; 
  } 

  public Vector allOneRoleNames()
  { Vector res = allAssociations(); 
    Vector oneroles = new Vector(); 
    for (int i = 0; i < res.size(); i++) 
    { Association ast = (Association) res.get(i); 
      if (ast.isSingleValued())
      { oneroles.add(ast.getRole2()); } 
    } 
    return oneroles; 
  } 
      
  public void setAssociations(Vector asts)
  { associations = asts; } 

  public void addAssociations(Vector assts)
  { for (int i = 0; i < assts.size(); i++) 
    { Association ast = (Association) assts.get(i); 
      if (associations.contains(ast)) { } 
      else
      { associations.add(ast); } 
    } 
  } 

  /* public void pushdownAssociations() // for interfaces only
  { if (isInterface()) {} else { return; } 

    for (int i = 0; i < subclasses.size(); i++) 
    { Entity ee = (Entity) subclasses.get(i); 
      ee.addAssociations(associations); 
    } 
  }  */ 


  public void setLinkedAssociation(Association myself)
  { linkedAssociation = myself; 
    if (myself != null) 
    { myself.setLinkedClass(this); 
      Entity e1 = myself.getEntity1(); 
      Entity e2 = myself.getEntity2(); 
      String r1nme = e1.getName().toLowerCase(); 
      String r2nme = e2.getName().toLowerCase(); 
      Association virtual1 = 
        new Association(this,e1,ModelElement.MANY,ModelElement.ONE,null,r1nme); 
      Association virtual2 = 
        new Association(this,e2,ModelElement.MANY,ModelElement.ONE,null,r2nme); 
      // addRole(r1nme,virtual1,e,entities,types,cons);
      // addRole(r2nme,virtual2,e,entities,types,cons);
      Vector virtuals = new Vector(); 
      virtuals.add(virtual1); 
      virtuals.add(virtual2);    
      addAssociations(virtuals); 
    } 
  } 

  public Vector getInvariants()
  { return invariants; } 

  public boolean hasSuperclass()
  { return superclass != null; } 

  public Entity getSuperclass() 
  { return superclass; } 

  public Vector getSubclasses()
  { return subclasses; } 

  public Vector getSuperclasses()
  { Vector res = new Vector(); 
    if (superclass != null) 
    { res.add(superclass); } 
    if (superclasses != null) 
    { res.addAll(superclasses); }  
    return res; 
  } 

  public Vector getAllSuperclasses()
  { Vector res = new Vector(); 
    if (superclass != null)
    { res.add(superclass); 
      res.addAll(superclass.getAllSuperclasses()); 
    } 
    return res; 
  } 

  public Entity getTopSuperclass()
  { if (superclass == null) 
    { return this; } 
    return superclass.getTopSuperclass(); 
  } 


  public Vector getAllSubclasses() // recursively
  { Vector seen = new Vector(); 
    seen.add(this); 
    return getAllSubclassesRec(seen); 
  } 

  public Vector getAllSubclassesRec(Vector seen) // recursively
  { Vector allsubs = new Vector(); 
    allsubs.addAll(subclasses);

    if (subclasses.contains(this))
    { System.err.println("!!! Error: cycle in inheritance " + getName()); 
      subclasses.remove(this); 
      return allsubs; 
    } 
 
    for (int i = 0; i < subclasses.size(); i++) 
    { Entity esub = (Entity) subclasses.get(i); 
      if (seen.contains(esub))
      { System.err.println("!!! Error: cycle in inheritance " + getName() + 
                           " subclass " + esub);
        return allsubs; 
      }  
      Vector newseen = new Vector(); 
      newseen.addAll(seen); 
      newseen.addAll(subclasses); 
      allsubs.addAll(esub.getAllSubclassesRec(newseen)); 
    } 
    return allsubs; 
  } 


  public Vector getAllConcreteSubclasses() // recursively
  { Vector res = new Vector(); 
    Vector subs = getAllSubclasses();
    for (int i = 0; i < subs.size(); i++) 
    { Entity sub = (Entity) subs.get(i); 
      if (sub.isConcrete())
      { res.add(sub); }  
    } 
    return res; 
  } 

  public Vector semanticCotopy() 
  { Vector res = new Vector(); 
    res.add(this); 
    res.addAll(getAllSuperclasses()); 
    return VectorUtil.union(res,getAllSubclasses()); 
  } 

  public Vector upperCotopy() 
  { Vector res = new Vector(); 
    res.add(this); 
    res.addAll(getAllSuperclasses()); 
    return res; 
  } 

  public double esim(Entity e, Map mm, ModelMatching modm, Vector entities)
  { // product_{f : features} sum_{g : e.features} asim(f,g)
    Vector sources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < localFeatures.size(); i++) 
      { Attribute att = (Attribute) localFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(localFeatures); } 
 
    Vector targets = new Vector(); 
    if (e.isShared())
    { Vector elocalfeatures = e.getLocalFeatures(); 
      for (int i = 0; i < elocalfeatures.size(); i++) 
      { Attribute att = (Attribute) elocalfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getLocalFeatures()); } 

    if (isShared())
    { for (int i = 0; i < nonlocalFeatures.size(); i++) 
      { Attribute att = (Attribute) nonlocalFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(nonlocalFeatures); } 

    if (e.isShared())
    { Vector eotherfeatures = e.getNonLocalFeatures(); 
      for (int i = 0; i < eotherfeatures.size(); i++) 
      { Attribute att = (Attribute) eotherfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getNonLocalFeatures()); } 

    double res = 1; 

    for (int i = 0; i < sources.size(); i++) 
    { Attribute satt = (Attribute) sources.get(i); 
      double ssum = 0; 
      for (int j = 0; j < targets.size(); j++) 
      { Attribute tatt = (Attribute) targets.get(j); 
        double simst = Attribute.asim(satt,tatt,mm,entities); 
        ssum = ssum + simst - (ssum*simst); 
      } 
      res = res*ssum; 
    } 
    return res; 
  } 

  public double esimN(Entity e, Map mm, ModelMatching modm, 
                      Vector entities, Vector thesaurus)
  { // product_{f : features} sum_{g : e.features} asim(f,g)*nsim(f,g)
    Vector sources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < localFeatures.size(); i++) 
      { Attribute att = (Attribute) localFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(localFeatures); } 
 
    Vector targets = new Vector(); 
    if (e.isShared())
    { Vector elocalfeatures = e.getLocalFeatures(); 
      for (int i = 0; i < elocalfeatures.size(); i++) 
      { Attribute att = (Attribute) elocalfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getLocalFeatures()); } 

    if (isShared())
    { for (int i = 0; i < nonlocalFeatures.size(); i++) 
      { Attribute att = (Attribute) nonlocalFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(nonlocalFeatures); } 

    if (e.isShared())
    { Vector eotherfeatures = e.getNonLocalFeatures(); 
      for (int i = 0; i < eotherfeatures.size(); i++) 
      { Attribute att = (Attribute) eotherfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getNonLocalFeatures()); } 

    double res = 1; 

    for (int i = 0; i < sources.size(); i++) 
    { Attribute satt = (Attribute) sources.get(i); 
      double ssum = 0; 
      for (int j = 0; j < targets.size(); j++) 
      { Attribute tatt = (Attribute) targets.get(j); 
        double namesim = ModelElement.similarity(satt.getName(), tatt.getName()); 
        double namesemsim = Entity.nmsSimilarity(satt.getName(), tatt.getName(), thesaurus); 
        double nsim = (namesim + namesemsim - namesim*namesemsim); 
        double simst = Attribute.asim(satt,tatt,mm,entities) * nsim; 
        ssum = ssum + simst - (ssum*simst); 
      } 
      res = res*ssum; 
    } 
    return res; 
  } 

  public double esimAbs(Entity e, Map mm, ModelMatching modm, Vector entities)
  { // product_{f : features} sum_{g : e.features} asim(f,g)
    Vector sources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < localFeatures.size(); i++) 
      { Attribute att = (Attribute) localFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(localFeatures); } 
 
    Vector targets = new Vector(); 
    if (e.isShared())
    { Vector elocalfeatures = e.getLocalFeatures(); 
      for (int i = 0; i < elocalfeatures.size(); i++) 
      { Attribute att = (Attribute) elocalfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getLocalFeatures()); } 

    if (isShared())
    { for (int i = 0; i < nonlocalFeatures.size(); i++) 
      { Attribute att = (Attribute) nonlocalFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(nonlocalFeatures); } 

    if (e.isShared())
    { Vector eotherfeatures = e.getNonLocalFeatures(); 
      for (int i = 0; i < eotherfeatures.size(); i++) 
      { Attribute att = (Attribute) eotherfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getNonLocalFeatures()); } 

    double res = 1; 

    for (int i = 0; i < targets.size(); i++) 
    { Attribute tatt = (Attribute) targets.get(i); 
      double ssum = 0; 
      for (int j = 0; j < sources.size(); j++) 
      { Attribute satt = (Attribute) sources.get(j); 
        double simst = Attribute.asim(satt,tatt,mm,entities); 
        ssum = ssum + simst - (ssum*simst); 
      } 
      res = res*ssum; 
    } 
    return res; 
  } 


  public double esimAbsN(Entity e, Map mm, ModelMatching modm, 
                         Vector entities, Vector thesaurus)
  { // product_{f : features} sum_{g : e.features} asim(f,g)
    Vector sources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < localFeatures.size(); i++) 
      { Attribute att = (Attribute) localFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(localFeatures); } 
 
    Vector targets = new Vector(); 
    if (e.isShared())
    { Vector elocalfeatures = e.getLocalFeatures(); 
      for (int i = 0; i < elocalfeatures.size(); i++) 
      { Attribute att = (Attribute) elocalfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getLocalFeatures()); } 

    if (isShared())
    { for (int i = 0; i < nonlocalFeatures.size(); i++) 
      { Attribute att = (Attribute) nonlocalFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(nonlocalFeatures); } 

    if (e.isShared())
    { Vector eotherfeatures = e.getNonLocalFeatures(); 
      for (int i = 0; i < eotherfeatures.size(); i++) 
      { Attribute att = (Attribute) eotherfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getNonLocalFeatures()); } 

    double res = 1; 

    for (int i = 0; i < targets.size(); i++) 
    { Attribute tatt = (Attribute) targets.get(i); 
      double ssum = 0; 
      for (int j = 0; j < sources.size(); j++) 
      { Attribute satt = (Attribute) sources.get(j); 
        double namesim = ModelElement.similarity(satt.getName(), tatt.getName()); 
        double namesemsim = Entity.nmsSimilarity(satt.getName(), tatt.getName(), thesaurus); 
        double nsim = (namesim + namesemsim - namesim*namesemsim); 
        double simst = Attribute.asim(satt,tatt,mm,entities) * nsim; 
        ssum = ssum + simst - (ssum*simst); 
      } 
      res = res*ssum; 
    } 
    return res; 
  } 

  public double esimForNSSNMS(Entity e, Map mm, ModelMatching modm, 
                              Vector entities, Vector thesaurus)
  { Vector sources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < localFeatures.size(); i++) 
      { Attribute att = (Attribute) localFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(localFeatures); } 
 
    Vector targets = new Vector(); 
    if (e.isShared())
    { Vector elocalfeatures = e.getLocalFeatures(); 
      for (int i = 0; i < elocalfeatures.size(); i++) 
      { Attribute att = (Attribute) elocalfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getLocalFeatures()); } 

    if (isShared())
    { for (int i = 0; i < nonlocalFeatures.size(); i++) 
      { Attribute att = (Attribute) nonlocalFeatures.get(i); 
        if (att.isSource())
        { sources.add(att); } 
      } 
    } 
    else 
    { sources.addAll(nonlocalFeatures); } 

    if (e.isShared())
    { Vector eotherfeatures = e.getNonLocalFeatures(); 
      for (int i = 0; i < eotherfeatures.size(); i++) 
      { Attribute att = (Attribute) eotherfeatures.get(i); 
        if (att.isTarget())
        { targets.add(att); } 
      } 
    } 
    else 
    { targets.addAll(e.getNonLocalFeatures()); } 

    double res = 1; 
    Vector matched = new Vector(); 
    Vector ematched = new Vector(); 
    Vector attmatches = new Vector(); 

    // Try NMS-matching the source attributes to targets: 

    for (int j = 0; j < sources.size(); j++) 
    { Attribute satt = (Attribute) sources.get(j); 
      double namesim = satt.findNMSMatch(targets, matched, attmatches, mm, entities, thesaurus); 
      res = res + namesim; 
    } 

    sources.removeAll(matched); 
    // targets.removeAll(ematched); 

    // Now try type-matching them

   Vector tmatched = new Vector(); 

    for (int j = 0; j < sources.size(); j++) 
    { Attribute satt = (Attribute) sources.get(j); 
      double namesim = satt.findTypeMatch(targets, tmatched, ematched, entities, mm); 
      res = res + namesim; 
    } 

    modm.setAttributeMatches(this,e,attmatches); 
    modm.addAttributeMatches(this,e,tmatched,ematched); 

    return res; 
  } 


  public double cotopySimilarity(Entity e, Map mm, ModelMatching modm, Vector entities) 
  { double res = 0; 
    double score = 0; 

    Vector matched = new Vector(); 
    Vector ematched = new Vector(); 

    Vector scotopy = upperCotopy(); 
    Vector tcotopy = e.upperCotopy(); 

    if (superclass != null) 
    { // if modm aleady has superclass mapping, use its attribute mappings. 
      Entity smatch = (Entity) mm.get(superclass); 
      if (smatch != null) 
      { if (e == smatch || isAncestor(smatch,e)) { } 
        else 
        { // System.out.println(">>> cannot match " + getName() + " to " + 
          //                    e.getName() + " : not descendent of superclass target " + 
          //                    smatch.getName()); 
          return 0; 
        } 

        EntityMatching em = modm.getEntityMatching(superclass); 
        if (em != null) 
        { Vector amaps = em.getAttributeMatchings(); 
          if (amaps != null) 
          { for (int i = 0; i < amaps.size(); i++) 
            { AttributeMatching am = (AttributeMatching) amaps.get(i); 
              matched.add(am.src); 
              ematched.add(am.trg); 
              score++; 
              // System.out.println(">>> Copied matches " + matched + " >>> " + ematched + 
              //                  " from " + superclass + " to " + this); 
            } 
          } 
        } 
        else 
        { System.err.println(">> No match for " + superclass + " in " + mm); } 
      } 
    } 
    else if (subclasses.size() > 0) 
    { for (int i = 0; i < subclasses.size(); i++) 
      { Entity sub = (Entity) subclasses.get(i); 
        Entity submatch = (Entity) mm.get(sub); 
        if (submatch != null) 
        { if (e == submatch || isAncestor(e,submatch)) { } 
          else 
          { // System.out.println(">>> cannot match " + getName() + " to " + 
            //                  e.getName() + " : not ancestor of subclass target " + 
            //                  submatch.getName()); 
            return 0;
          }  
        } 
      } // copy attribute matches to the sub - or done in correlation pattern analysis 
    } 

    Vector invcotopy = mm.inverseImage(tcotopy); 
    Vector intersect = VectorUtil.intersection(scotopy,invcotopy); 
    int intsize = intersect.size(); 
    Vector uni = VectorUtil.union(scotopy,invcotopy); 
    int unisize = uni.size(); 
    if (unisize == 0) 
    { res = 1; } 
    else  
    { res = (1.0*intsize)/unisize; } 

    // Vector atts = allDefinedAssociations(); 
    // Vector tatts = e.allDefinedAssociations(); 

    // Vector batts = allDefinedAttributes(); 
    // Vector btatts = e.allDefinedAttributes(); 


    // if (atts.size() == 0 && tatts.size() == 0 && batts.size() == 0 && btatts.size() == 0) 
    // { return res; } 


    Vector localsources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < localFeatures.size(); i++) 
      { Attribute att = (Attribute) localFeatures.get(i); 
        if (att.isSource())
        { localsources.add(att); } 
      } 
    } 
    else 
    { localsources.addAll(localFeatures); } 

    // System.out.println(">>**>> Local sources of " + getName() + " are: " + localsources); 

 
    Vector localtargets = new Vector(); 
    if (e.isShared())
    { Vector elocalfeatures = e.getLocalFeatures(); 
      for (int i = 0; i < elocalfeatures.size(); i++) 
      { Attribute att = (Attribute) elocalfeatures.get(i); 
        if (att.isTarget())
        { localtargets.add(att); } 
      } 
    } 
    else 
    { localtargets.addAll(e.getLocalFeatures()); } 
 
    Vector othersources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < nonlocalFeatures.size(); i++) 
      { Attribute att = (Attribute) nonlocalFeatures.get(i); 
        if (att.isSource())
        { othersources.add(att); } 
      } 
    } 
    else 
    { othersources.addAll(nonlocalFeatures); } 
 
    Vector othertargets = new Vector(); 
    if (e.isShared())
    { Vector eotherfeatures = e.getNonLocalTargetFeatures(mm); 
      for (int i = 0; i < eotherfeatures.size(); i++) 
      { Attribute att = (Attribute) eotherfeatures.get(i); 
        if (att.isTarget())
        { othertargets.add(att); } 
      } 
    } 
    else 
    { othertargets.addAll(e.getNonLocalTargetFeatures(mm)); } 

    double totalFeatures = (localsources.size() + localtargets.size() + 
                             othersources.size() + othertargets.size()); 

    if (totalFeatures == 0) { return res; } 

    // Try to match local targets to local sources

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possibleExactMatches = new Vector(); 
 
      for (int i = 0; i < localsources.size(); i++) 
      { Attribute satt = (Attribute) localsources.get(i); 

        // System.out.println("Checking " + satt + " " + satt.getType() + " " + tatt + 
        //                    " " + tatt.getType()); 

        if (matched.contains(satt)) { } 
        else if (Attribute.exactTypeMatch(satt,tatt,mm))
        { if (satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score++; 
            continue; 
          } 
          else 
          { possibleExactMatches.add(satt); } 
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possibleExactMatches.size(); k++) 
      { Attribute a1 = (Attribute) possibleExactMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(tatt)) 
      { matched.add(best); 
        ematched.add(tatt); 
        score++; 
      } 
      // if (ast.isTarget()) { continue; } 
      // if (isShared() && !ast.isSource()) { continue; } 

      // Entity e2 = ast.getEntity2(); 
      // Entity me2 = (Entity) mm.get(e2); 
      // if (me2 != null) 
      // { int j = 0; 
      //   boolean notfound = true; 

      //   while (j < tatts.size() && notfound) 
      //   { Association tast = (Association) tatts.get(j); 
      //     Attribute tdatt = new Attribute(tast); 

      //     if (tast.isSource()) { } 
      //     else if (ematched.contains(tdatt)) { } 
      //     else if (tast.getEntity2() == me2 || isAncestor(tast.getEntity2(),me2))
      //     { score++; 
      //       notfound = false; 
      //       matched.add(datt); 
      //       ematched.add(tdatt); 
      //     } 
      //     j++; 
      //   } 
      // } 
    } 

    localtargets.removeAll(ematched); 

    // Now try to match local targets to non-local sources

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possibleExactMatches = new Vector(); 
 
      for (int i = 0; i < othersources.size(); i++) 
      { Attribute satt = (Attribute) othersources.get(i); 
        if (matched.contains(satt)) { } 
        else if (Attribute.exactTypeMatch(satt,tatt,mm))
        { if (satt.getName().equals(tatt.getName()))
          { matched.add(satt); 
            ematched.add(tatt); 
            score++; 
            continue; 
          } 
          else 
          { possibleExactMatches.add(satt); } 
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possibleExactMatches.size(); k++) 
      { Attribute a1 = (Attribute) possibleExactMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 
      if (best != null) 
      { matched.add(best); 
        ematched.add(tatt); 
        score++; 
      } 
    } 

    // Then, exact matches of unused local sources to non-local targets: 

    localsources.removeAll(matched); 

    for (int j = 0; j < localsources.size(); j++) 
    { Attribute satt = (Attribute) localsources.get(j); 

      if (matched.contains(satt)) { continue; }
      
      Vector possibleExactMatches = new Vector(); 
 
      for (int i = 0; i < othertargets.size(); i++) 
      { Attribute tatt = (Attribute) othertargets.get(i); 
        if (ematched.contains(tatt)) { } 
        else if (Attribute.exactTypeMatch(satt,tatt,mm))
        { if (satt.getName().equals(tatt.getName()))
          { matched.add(satt); 
            ematched.add(tatt); 
            score++; 
            continue; 
          } 
          else 
          { possibleExactMatches.add(tatt); } 
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possibleExactMatches.size(); k++) 
      { Attribute a1 = (Attribute) possibleExactMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), satt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 
      if (best != null) 
      { matched.add(satt); 
        ematched.add(best); 
        score++; 
      } 
    } 

    // Try to partially match local targets to local sources

    localtargets.removeAll(ematched); 
    localsources.removeAll(matched); 

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possiblePartialMatches = new Vector(); 
      double bestd = 0; 
 
      for (int i = 0; i < localsources.size(); i++) 
      { Attribute satt = (Attribute) localsources.get(i); 

        // System.out.println("Checking " + satt + " " + satt.getType() + " " + tatt + 
        //                    " " + tatt.getType()); 

        if (matched.contains(satt)) { } 
        else 
        { double matchd = Attribute.partialTypeMatch(satt,tatt,mm,entities); 
          if (matchd > 0 && satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score = score + matchd; 
            continue; 
          } 
          else if (matchd > bestd)
          { possiblePartialMatches.clear(); 
            possiblePartialMatches.add(satt); 
            bestd = matchd; 
          }
          else if (matchd > 0 && matchd == bestd)
          { possiblePartialMatches.add(satt); }              
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possiblePartialMatches.size(); k++) 
      { Attribute a1 = (Attribute) possiblePartialMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(tatt)) 
      { matched.add(best); 
        ematched.add(tatt); 
        score = score + bestd; 
      } 
    } 

    // Try to partially match local targets to non-local sources

    localtargets.removeAll(ematched); 
    othersources.removeAll(matched); 

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possiblePartialMatches = new Vector(); 
      double bestd = 0; 
 
      for (int i = 0; i < othersources.size(); i++) 
      { Attribute satt = (Attribute) othersources.get(i); 

        // System.out.println("Checking " + satt + " " + satt.getType() + " " + tatt + 
        //                    " " + tatt.getType()); 

        if (matched.contains(satt)) { } 
        else 
        { double matchd = Attribute.partialTypeMatch(satt,tatt,mm,entities); 
          if (matchd > 0 && satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score = score + matchd; 
            continue; 
          } 
          else if (matchd > bestd)
          { possiblePartialMatches.clear(); 
            possiblePartialMatches.add(satt); 
            bestd = matchd; 
          }
          else if (matchd > 0 && matchd == bestd)
          { possiblePartialMatches.add(satt); }              
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possiblePartialMatches.size(); k++) 
      { Attribute a1 = (Attribute) possiblePartialMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(tatt)) 
      { matched.add(best); 
        ematched.add(tatt); 
        score = score + bestd; 
      } 
    } 

    // Try to partially match unused local sources to non-local targets

    othertargets.removeAll(ematched); 
    localsources.removeAll(matched); 

    for (int j = 0; j < localsources.size(); j++) 
    { Attribute satt = (Attribute) localsources.get(j); 

      if (matched.contains(satt)) { continue; }
      
      Vector possiblePartialMatches = new Vector(); 
      double bestd = 0; 
 
      for (int i = 0; i < othertargets.size(); i++) 
      { Attribute tatt = (Attribute) othertargets.get(i); 

        /* System.out.println("Checking unused source feature " + satt + " " + satt.getType() + 
                           "(" + satt.getElementType() + ") " + 
                           satt.getType().isEntity() + " " + satt.getElementType().isEntity() + 
                           " TO>> " + tatt + 
                           " " + tatt.getType() + "(" + tatt.getElementType() + 
                           ")" + tatt.getType().isEntity() + " " + tatt.getElementType().isEntity()); 
        */ 

        if (ematched.contains(tatt)) { } 
        else 
        { double matchd = Attribute.partialTypeMatch(satt,tatt,mm,entities); 
          // System.out.println("Matching is " + matchd); 

          if (matchd > 0 && satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score = score + matchd; 
            continue; 
          } 
          else if (matchd > bestd)
          { possiblePartialMatches.clear(); 
            possiblePartialMatches.add(tatt); 
            bestd = matchd; 
          }
          else if (matchd > 0 && matchd == bestd)
          { possiblePartialMatches.add(tatt); }              
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possiblePartialMatches.size(); k++) 
      { Attribute a1 = (Attribute) possiblePartialMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), satt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(best)) 
      { ematched.add(best); 
        matched.add(satt); 
        score = score + bestd; 
      } 
    } 
    
   /*  for (int k = 0; k < batts.size(); k++) 
    { Attribute att = (Attribute) batts.get(k); 

      if (matched.contains(att)) { continue; } 
      if (att.isTarget()) { continue; } 
      if (isShared() && !att.isSource()) { continue; } 

      Type typeatt = att.getType(); 
      int p = 0; 
      boolean nfound = true; 

      while (p < btatts.size() && nfound) 
      { Attribute tatt = (Attribute) btatts.get(p);
        Type typetatt = tatt.getType(); 
 
        if (tatt.isSource()) { } 
        else if (ematched.contains(tatt)) { } 
        else if (Type.isSubType(typeatt,typetatt))
        { score++; 
          nfound = false; 
          matched.add(att); 
          ematched.add(tatt); 
        } 
        p++; 
      } 
    } */ 

    modm.setAttributeMatches(this,e,matched,ematched); 
 
    res = res + (score*1.0)/totalFeatures; 
    return res; 
  } 

  public double compositeSimilarity(Entity e, Map mm, ModelMatching modm, Vector entities) 
  { double res = 0; 
    double score = 0; 

    Vector matched = new Vector(); 
    Vector ematched = new Vector(); 



    // Vector atts = allDefinedAssociations(); 
    // Vector tatts = e.allDefinedAssociations(); 

    // Vector batts = allDefinedAttributes(); 
    // Vector btatts = e.allDefinedAttributes(); 


    // if (atts.size() == 0 && tatts.size() == 0 && batts.size() == 0 && btatts.size() == 0) 
    // { return res; } 


    Vector localsources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < localFeatures.size(); i++) 
      { Attribute att = (Attribute) localFeatures.get(i); 
        if (att.isSource())
        { localsources.add(att); } 
      } 
    } 
    else 
    { localsources.addAll(localFeatures); } 

    // System.out.println(">>**>> Local sources of " + getName() + " are: " + localsources); 

 
    Vector localtargets = new Vector(); 
    if (e.isShared())
    { Vector elocalfeatures = e.getLocalFeatures(); 
      for (int i = 0; i < elocalfeatures.size(); i++) 
      { Attribute att = (Attribute) elocalfeatures.get(i); 
        if (att.isTarget())
        { localtargets.add(att); } 
      } 
    } 
    else 
    { localtargets.addAll(e.getLocalFeatures()); } 
 
    Vector othersources = new Vector(); 
    if (isShared())
    { for (int i = 0; i < nonlocalFeatures.size(); i++) 
      { Attribute att = (Attribute) nonlocalFeatures.get(i); 
        if (att.isSource())
        { othersources.add(att); } 
      } 
    } 
    else 
    { othersources.addAll(nonlocalFeatures); } 
 
    Vector othertargets = new Vector(); 
    if (e.isShared())
    { Vector eotherfeatures = e.getNonLocalTargetFeatures(mm); 
      for (int i = 0; i < eotherfeatures.size(); i++) 
      { Attribute att = (Attribute) eotherfeatures.get(i); 
        if (att.isTarget())
        { othertargets.add(att); } 
      } 
    } 
    else 
    { othertargets.addAll(e.getNonLocalTargetFeatures(mm)); } 

    double totalFeatures = (localsources.size() + localtargets.size() + 
                             othersources.size() + othertargets.size()); 

    if (totalFeatures == 0) { return res; } 

    // Try to match local targets to local sources

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possibleExactMatches = new Vector(); 
 
      for (int i = 0; i < localsources.size(); i++) 
      { Attribute satt = (Attribute) localsources.get(i); 

        // System.out.println("Checking " + satt + " " + satt.getType() + " " + tatt + 
        //                    " " + tatt.getType()); 

        if (matched.contains(satt)) { } 
        else if (Attribute.exactTypeMatch(satt,tatt,mm))
        { if (satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score++; 
            continue; 
          } 
          else 
          { possibleExactMatches.add(satt); } 
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possibleExactMatches.size(); k++) 
      { Attribute a1 = (Attribute) possibleExactMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(tatt)) 
      { matched.add(best); 
        ematched.add(tatt); 
        score++; 
      } 
    } 

    localtargets.removeAll(ematched); 

    // Now try to match local targets to non-local sources

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possibleExactMatches = new Vector(); 
 
      for (int i = 0; i < othersources.size(); i++) 
      { Attribute satt = (Attribute) othersources.get(i); 
        if (matched.contains(satt)) { } 
        else if (ematched.contains(tatt)) { } 
        else if (Attribute.exactTypeMatch(satt,tatt,mm))
        { if (satt.getName().equals(tatt.getName()))
          { matched.add(satt); 
            ematched.add(tatt); 
            score++; 
            continue; 
          } 
          else 
          { possibleExactMatches.add(satt); } 
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possibleExactMatches.size(); k++) 
      { Attribute a1 = (Attribute) possibleExactMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 
      if (best != null) 
      { matched.add(best); 
        ematched.add(tatt); 
        score++; 
      } 
    } 

    // Then, exact matches of unused local sources to non-local targets: 

    localsources.removeAll(matched); 

    for (int j = 0; j < localsources.size(); j++) 
    { Attribute satt = (Attribute) localsources.get(j); 

      if (matched.contains(satt)) { continue; }
      
      Vector possibleExactMatches = new Vector(); 
 
      for (int i = 0; i < othertargets.size(); i++) 
      { Attribute tatt = (Attribute) othertargets.get(i); 
        if (ematched.contains(tatt)) { } 
        else if (matched.contains(satt)) { } 
        else if (Attribute.exactTypeMatch(satt,tatt,mm))
        { if (satt.getName().equals(tatt.getName()))
          { matched.add(satt); 
            ematched.add(tatt); 
            score++; 
            continue; 
          } 
          else 
          { possibleExactMatches.add(tatt); } 
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possibleExactMatches.size(); k++) 
      { Attribute a1 = (Attribute) possibleExactMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), satt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 
      if (best != null) 
      { matched.add(satt); 
        ematched.add(best); 
        score++; 
      } 
    } 

    // Try to partially match local targets to local sources

    localtargets.removeAll(ematched); 
    localsources.removeAll(matched); 

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possiblePartialMatches = new Vector(); 
      double bestd = 0; 
 
      for (int i = 0; i < localsources.size(); i++) 
      { Attribute satt = (Attribute) localsources.get(i); 

        // System.out.println("Checking " + satt + " " + satt.getType() + " " + tatt + 
        //                    " " + tatt.getType()); 

        if (matched.contains(satt)) { } 
        else if (ematched.contains(tatt)) { } 
        else 
        { double matchd = Attribute.partialTypeMatch(satt,tatt,mm,entities); 
          if (matchd > 0 && satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score = score + matchd; 
            continue; 
          } 
          else if (matchd > bestd)
          { possiblePartialMatches.clear(); 
            possiblePartialMatches.add(satt); 
            bestd = matchd; 
          }
          else if (matchd > 0 && matchd == bestd)
          { possiblePartialMatches.add(satt); }              
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possiblePartialMatches.size(); k++) 
      { Attribute a1 = (Attribute) possiblePartialMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(tatt)) 
      { matched.add(best); 
        ematched.add(tatt); 
        score = score + bestd; 
      } 
    } 

    // Try to partially match local targets to non-local sources

    localtargets.removeAll(ematched); 
    othersources.removeAll(matched); 

    for (int j = 0; j < localtargets.size(); j++) 
    { Attribute tatt = (Attribute) localtargets.get(j); 

      if (ematched.contains(tatt)) { continue; }
      
      Vector possiblePartialMatches = new Vector(); 
      double bestd = 0; 
 
      for (int i = 0; i < othersources.size(); i++) 
      { Attribute satt = (Attribute) othersources.get(i); 

        // System.out.println("Checking " + satt + " " + satt.getType() + " " + tatt + 
        //                    " " + tatt.getType()); 

        if (matched.contains(satt)) { } 
        else if (ematched.contains(tatt)) { } 
        else 
        { double matchd = Attribute.partialTypeMatch(satt,tatt,mm,entities); 
          if (matchd > 0 && satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score = score + matchd; 
            continue; 
          } 
          else if (matchd > bestd)
          { possiblePartialMatches.clear(); 
            possiblePartialMatches.add(satt); 
            bestd = matchd; 
          }
          else if (matchd > 0 && matchd == bestd)
          { possiblePartialMatches.add(satt); }              
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possiblePartialMatches.size(); k++) 
      { Attribute a1 = (Attribute) possiblePartialMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), tatt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(tatt)) 
      { matched.add(best); 
        ematched.add(tatt); 
        score = score + bestd; 
      } 
    } 

    // Try to partially match unused local sources to non-local targets

    othertargets.removeAll(ematched); 
    localsources.removeAll(matched); 

    for (int j = 0; j < localsources.size(); j++) 
    { Attribute satt = (Attribute) localsources.get(j); 

      if (matched.contains(satt)) { continue; }
      
      Vector possiblePartialMatches = new Vector(); 
      double bestd = 0; 
 
      for (int i = 0; i < othertargets.size(); i++) 
      { Attribute tatt = (Attribute) othertargets.get(i); 

        /* System.out.println("Checking unused source feature " + satt + " " + satt.getType() + 
                           "(" + satt.getElementType() + ") " + 
                           satt.getType().isEntity() + " " + satt.getElementType().isEntity() + 
                           " TO>> " + tatt + 
                           " " + tatt.getType() + "(" + tatt.getElementType() + 
                           ")" + tatt.getType().isEntity() + " " + tatt.getElementType().isEntity()); 
        */ 

        if (ematched.contains(tatt)) { }
        else 
        { double matchd = Attribute.partialTypeMatch(satt,tatt,mm,entities); 
          // System.out.println("Matching is " + matchd); 

          if (matchd > 0 && satt.getName().equals(tatt.getName()) && !ematched.contains(tatt))
          { matched.add(satt); 
            ematched.add(tatt); 
            score = score + matchd; 
            continue; 
          } 
          else if (matchd > bestd)
          { possiblePartialMatches.clear(); 
            possiblePartialMatches.add(tatt); 
            bestd = matchd; 
          }
          else if (matchd > 0 && matchd == bestd)
          { possiblePartialMatches.add(tatt); }              
        } 
      } 

      double bestsim = 0; 
      Attribute best = null; 
    
      for (int k = 0; k < possiblePartialMatches.size(); k++) 
      { Attribute a1 = (Attribute) possiblePartialMatches.get(k); 
        double d = ModelElement.similarity(a1.getName(), satt.getName()); 
        if (d > bestsim) 
        { bestsim = d; 
          best = a1; 
        } 
      } 

      if (best != null && !ematched.contains(best)) 
      { ematched.add(best); 
        matched.add(satt); 
        score = score + bestd; 
      } 
    } 

    if (superclass != null && modm != null) 
    { // if modm aleady has superclass mapping, use its attribute mappings. 
      Entity smatch = (Entity) mm.get(superclass); 
      if (smatch != null) 
      { if (e == smatch || isAncestor(smatch,e)) { } 
        else 
        { // System.out.println(">>> cannot match " + getName() + " to " + 
          //                    e.getName() + " : not descendent of superclass target " + 
          //                    smatch.getName()); 
          return 0; 
        } 

        EntityMatching em = modm.getEntityMatching(superclass); 
        if (em != null) 
        { Vector amaps = em.getAttributeMatchings(); 
          if (amaps != null) 
          { for (int i = 0; i < amaps.size(); i++) 
            { AttributeMatching am = (AttributeMatching) amaps.get(i); 
              if (ematched.contains(am.trg))
              { // if current match better than am, use it, otherwise use am
                Attribute tatt = am.trg; 
                int xindex = ematched.indexOf(tatt);
                Attribute satt = (Attribute) matched.get(xindex); 
 
                if (satt != null && Attribute.exactTypeMatch(satt,tatt,mm)) {}
                else  
                { if (matched.contains(am.src))
                  { int yindex = matched.indexOf(am.src); 
                    matched.set(xindex,am.src); 
                    // ematched.set(xindex,am.trg);
                    if (yindex != xindex)
                    { matched.remove(yindex); 
                      ematched.remove(yindex);
                    }  
                  } 
                  else 
                  { matched.set(xindex,am.src); 
                    // ematched.set(xindex,am.trg);
                  } 
                  System.out.println(">>> Copied better match " + am.src + " >>> " + am.trg + 
                                     " from " + superclass + " to " + this);
                } 
              } 
              else if (matched.contains(am.src)) { } 
              else 
              { matched.add(am.src); 
                ematched.add(am.trg); 
               
                score++; 
                System.out.println(">>> Copied match " + am.src + " >>> " + am.trg + 
                                   " from " + superclass + " to " + this);
              }  
            } 
          } 
        } 
        else 
        { System.err.println(">> No match for " + superclass + " in " + mm); } 
      } 
    } 
    else if (subclasses.size() > 0) 
    { for (int i = 0; i < subclasses.size(); i++) 
      { Entity sub = (Entity) subclasses.get(i); 
        Entity submatch = (Entity) mm.get(sub); 
        if (submatch != null) 
        { if (e == submatch || isAncestor(e,submatch)) { } 
          else 
          { // System.out.println(">>> cannot match " + getName() + " to " + 
            //                  e.getName() + " : not ancestor of subclass target " + 
            //                  submatch.getName()); 
            return 0;
          }  
        } 
      } // copy attribute matches to the sub - or done in correlation pattern analysis 
    } 
    
    if (modm != null) 
    { modm.setAttributeMatches(this,e,matched,ematched); } 
 
    res = res + (score*1.0)/totalFeatures; 
    return res; 
  } 


  public double nmsSimilarity(Entity e, Vector thesaurus) 
  { double res = 0; 
    String fnme = getName(); 
    String fenme = e.getName(); 
    return nmsSimilarity(fnme,fenme,thesaurus); 
  } 

  public double nms$Similarity(Entity e, Vector thesaurus) 
  { double res = 0; 
    String fnme = getName();
    String shortfname = fnme.substring(0,fnme.length()-1);  
    String fenme = e.getName(); 
    String shortename = fenme.substring(0,fenme.length()-1); 
    return nmsSimilarity(shortfname,shortename,thesaurus); 
  } 

  public static double nmsSimilarity(String fnme, String fenme, Vector thesaurus) 
  { double totalscore = 0; 

    totalscore = Thesarus.findSimilarity(fnme,fenme,thesaurus); 

    // if (totalscore > 0) 
    // { return totalscore; } 

    Vector w1 = ModelElement.splitIntoWords(fnme); 
    Vector w2 = ModelElement.splitIntoWords(fenme); 

    for (int i = 0; i < w1.size(); i++) 
    { String word1 = (String) w1.get(i); 
      for (int j = 0; j < w2.size(); j++) 
      { String word2 = (String) w2.get(j); 
        double wscore = Thesarus.findSimilarity(word1,word2,thesaurus); 
        totalscore = (totalscore + wscore) - (totalscore*wscore); 
      } 
    } 

    int res = (int) Math.round(totalscore*1000); 
    return res/1000.0; 
  } // also break up the names into parts

  public Vector getActualLeafSubclasses() // recursively
  { Vector res = new Vector(); 
    Vector allsubs = getAllSubclasses(); 
    for (int i = 0; i < allsubs.size(); i++) 
    { Entity esub = (Entity) allsubs.get(i); 
      if (esub.isActualLeaf())
      { res.add(esub); }
    } 
    return res; 
  } 

  public static Vector allLeafClasses(Vector entities)
  { Vector res = new Vector(); 
    for (int i = 0; i < entities.size(); i++) 
    { Entity e = (Entity) entities.get(i); 
      if (e.isActualLeaf())
      { res.add(e); } 
    } 
    return res; 
  } 

  public String objectType()
  { if (superclass != null) 
    { return superclass.objectType(); } 
    return getName() + "_OBJ"; 
  } 

  public Vector allDataDependents(Vector assocs)
  { // all superclasses, and all associations with this as entity2
    Vector res = new Vector(); 
    if (superclass != null)
    { res.add(superclass.getName()); 
      res.addAll(superclass.allDataDependents(assocs));
    } 
    // Vector ss = getAllSuperclasses(); 
    // for (int i = 0; i < ss.size(); i++) 
    // { Entity es = (Entity) ss.get(i); 
    //   res.add(es.getName()); 
    // } 

    for (int j = 0; j < assocs.size(); j++) 
    { Association ast = (Association) assocs.get(j); 
      Entity e1 = ast.getEntity1(); 
      Entity e2 = ast.getEntity2(); 
      String r1 = ast.getRole1(); 
      String r2 = ast.getRole2(); 

      if (this == e2)
      { if (res.contains(e1 + "::" + r2)) { } 
        else 
        { res.add(e1 + "::" + r2); }
        if (r1 != null && r1.length() > 0)
        { if (res.contains(e2 + "::" + r1)) { } 
          else 
          { res.add(e2 + "::" + r1); } 
        } 
      }  
      if (this == e1)
      { if (r1 != null && r1.length() > 0)
        { if (res.contains(e2 + "::" + r1)) { } 
          else 
          { res.add(e2 + "::" + r1); } 
          if (res.contains(e1 + "::" + r2)) { } 
          else 
          { res.add(e1 + "::" + r2); }
        } 
      } 
    } 

    return res; 
  } 


  public Vector ancestorAddStatements(BExpression var)
  { Vector res = new Vector(); 
    String nme = getName(); 
    String dvars = nme.toLowerCase() + "s";
    BasicExpression typ = new BasicExpression(dvars); 
    typ.setMultiplicity(ModelElement.MANY);  
    res.add(typ.constructLocalBOp("add",dvars,null,var,null,null)); 
    if (superclass != null)
    { Vector res1 = superclass.ancestorAddStatements(var); 
      res.addAll(res1); 
    } 
    return res; 
  } 

  public void listOperations(PrintWriter out)
  { for (int i = 0; i < operations.size(); i++)
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i);
      out.println(op.display() + "\n"); 
    } 
  } 
      
  public void displayMeasures(PrintWriter out, java.util.Map clones)
  { out.println("*** Class " + getName()); 

    int highcount = 0; 
    int lowcount = 0; 

    int atts = attributes.size(); 
    int assocs = associations.size(); 
    int ops = operations.size(); 

    out.println("*** Number of attributes = " + atts); 
    out.println("*** Number of roles = " + assocs); 
    out.println("*** Number of operations = " + ops); 

    int totalComplexity = 0; 

    for (int i = 0; i < ops; i++)
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i);
      
      int opcomplexity = op.displayMeasures(out);

      if (opcomplexity > 100) 
      { highcount++; } 
      else if (opcomplexity > 50)
      { lowcount++; } 

      totalComplexity = totalComplexity + opcomplexity;  
      Vector opuses = op.operationsUsedIn(); 
      if (opuses.size() > 0)
      { out.println("*** Operations used in " + op.getName() + " are: " + opuses); } 
      op.findClones(clones); 
      out.println(); 
      
    } 

    out.println("*** " + highcount + " operations of " + getName() + " are > 100 complexity"); 
    out.println("*** " + lowcount + " other operations of " + getName() + " are > 50 complexity"); 

    out.println("*** Total complexity of " + getName() + " is: " + totalComplexity); 
  } 

  public int excessiveOperationsSize()
  { // number of operations over limit of 100
    int res = 0; 
    int ops = operations.size();
 
    for (int i = 0; i < ops; i++)
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i);
      int opc = op.syntacticComplexity(); 
      if (opc > 100) 
      { res++; } 
    } 
    return res; 
  }       


  public Map getCallGraph()
  { Map res = new Map(); 

    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      String opname = op.getName(); 

      Vector opuses = op.operationsUsedIn(); 

      if (opuses.size() > 5) 
      { System.err.println("*** Bad smell: > 5 operations used in " + getName() + "::" + opname); } 

      String entop = getName() + "::" + opname; 
      for (int j = 0; j < opuses.size(); j++) 
      { res.add_pair(entop, opuses.get(j)); } 

      if (superclass != null) 
      { Entity sup = superclass; 
        while (sup != null) 
        { if (sup.hasOperation(opname)) 
          { res.add_pair(sup.getName() + "::" + opname, entop); 
            break; 
          } 
          else 
          { sup = sup.superclass; } 
        } 
      } 
    } 

    return res; 
  } 


  public Vector allLhsFeatures()
  { // all features used in any lhs of a local invariant
    Vector res = new Vector(); 
    for (int i = 0; i < invariants.size(); i++)
    { Constraint c = (Constraint) invariants.get(i); 
      res.addAll(c.allLhsFeatures()); 
    } 
    return res; 
  }  
 
  public Vector allRhsFeatures()
  { // all features used in any rhs of a local invariant
    Vector res = new Vector(); 
    for (int i = 0; i < invariants.size(); i++)
    { Constraint c = (Constraint) invariants.get(i); 
      res.addAll(c.allRhsFeatures()); 
    } 
    return res; 
  }  
      
  public Vector allLhsValues()
  { // all values used in any lhs of a local invariant
    Vector res = new Vector(); 
    for (int i = 0; i < invariants.size(); i++)
    { Constraint c = (Constraint) invariants.get(i); 
      res.addAll(c.allLhsValues()); 
    } 
    return res; 
  }  
 
  public Vector allRhsValues()
  { // all values used in any rhs of a local invariant
    Vector res = new Vector(); 
    for (int i = 0; i < invariants.size(); i++)
    { Constraint c = (Constraint) invariants.get(i); 
      res.addAll(c.allRhsValues()); 
    } 
    return res; 
  }  
      
  public void setSuperclass(Entity s)
  { superclass = s; } 

  public void addSuperclass(Entity e) 
  { if (superclasses == null) 
    { superclasses = new Vector(); } 
    superclasses.add(e); 
  } 

  public void addSubclass(Entity s)
  { if (isLeaf())
    { System.err.println("Leaf entities cannot have subclasses"); 
      return; 
    }
    if (subclasses.contains(s)) { } 
    else 
    { subclasses.add(s); }  
  } 

  public void removeSubclass(Entity e)
  { subclasses.remove(e); } 

  public void addInvariant(Constraint cons) 
  { invariants.add(cons); } 

  public void addInvariants(Vector invs)
  { invariants.addAll(invs); } 

  public void createPrimaryKey()
  { String key = getName().toLowerCase() + "Id"; 
    if (hasAttribute(key))
    { System.err.println("Cannot create attribute with name: " + key); } 
    else 
    { Type tint = new Type("String",null);   // not int 
      Attribute att = new Attribute(key,tint,ModelElement.INTERNAL); 
      att.setUnique(true); 
      att.setEntity(this); 
      attributes.add(0,att); 
    } 
  }

  public boolean hasAttribute(String att)
  { return ModelElement.lookupByName(att,attributes) != null; } 

  public boolean hasInheritedAttribute(String att)
  { boolean res = false; 
    if (superclass != null)
    { res = superclass.hasInheritedAttribute(att); } 
    res = res || hasAttribute(att); 
    return res;
  }
  /* if (superclass != null) 
     { return (superclass.hasAttribute(att) || superclass.hasInheritedAttribute(att)); } 
     return false; 
   */ 

  public boolean hasDefinedAttribute(String att)
  { if (hasAttribute(att))
    { return true; } 
    return hasInheritedAttribute(att); 
  } 

  public Vector allInheritedAttributes()
  { Vector res = new Vector(); 
    if (superclass == null) 
    { return res; } 
    res.addAll(superclass.getAttributes()); 
    res.addAll(superclass.allInheritedAttributes()); 
    return res; 
  } 

  public Vector allDefinedAttributes()
  { Vector res = new Vector(); 
    res.addAll(attributes); 
    res.addAll(allInheritedAttributes()); 
    return res; 
  } 

  public boolean isDefinedDataFeature(Attribute att) 
  { String attname = att.getName(); 
    if (hasAttribute(attname))
    { return true; } 
    if (ModelElement.lookupByName(attname,localFeatures) != null)
    { return true; } 
    if (ModelElement.lookupByName(attname,nonlocalFeatures) != null)
    { return true; } 
    return false; 
  } 
         

  public Vector allOperations()
  { Vector res = new Vector(); // BehaviouralFeature
    res.addAll(operations); 
    if (superclass != null) 
    { res.addAll(superclass.allOperations()); } 
    return res; 
  } 
 
  public Vector allInheritedAssociations()
  { Vector res = new Vector(); 
    if (superclass == null) 
    { return res; } 
    res.addAll(superclass.getAssociations()); 
    res.addAll(superclass.allInheritedAssociations()); 
    return res; 
  } 

  public Vector allDefinedAssociations()
  { Vector res = new Vector(); 
    res.addAll(associations); 
    res.addAll(allInheritedAssociations()); 
    return res; 
  } 

  public Vector allDataFeatures()
  { // either from superclass or interfaces
    Vector res = new Vector(); 
    res.addAll(attributes); 
    res.addAll(associations); 
    return res; 
  } 

  public Vector allInheritedDataFeaturesCPP()
  { // either from superclass or interfaces
    Vector res = new Vector(); 
    if (superclass != null) 
    { res.addAll(superclass.allDataFeatures()); 
      res.addAll(superclass.allInheritedDataFeaturesCPP());
    } 
    for (int i = 0; i < interfaces.size(); i++) 
    { Entity inf = (Entity) interfaces.get(i); 
      res.addAll(inf.allDataFeatures()); 
      res.addAll(inf.allInheritedDataFeaturesCPP());
    } 
    return res; 
  } 

  public static Entity firstDefiningClass(Vector supclasses, ModelElement feat)
  { for (int i = 0; i < supclasses.size(); i++) 
    { Entity sup = (Entity) supclasses.get(i); 
      if (sup.allDataFeatures().contains(feat)) { return sup; } 
      if (sup.allInheritedDataFeaturesCPP().contains(feat)) { return sup; } 
    } 
    return null; 
  } 

  public boolean hasRole(String rle)
  { if (rle == null) 
    { return false; } 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      String role2 = ast.getRole2(); 
      if (rle.equals(role2))
      { return true; } 
    } 
    return false; 
  } 

  public boolean hasDefinedRole(String rle)
  { if (hasRole(rle))
    { return true; } 
    if (superclass != null) 
    { return superclass.hasDefinedRole(rle); } 
    return false; 
  } 


  public boolean hasFeature(String f)
  { boolean res = hasAttribute(f); 
    if (res == true) { return res; } 
    res = hasRole(f); 
    if (res == true) { return res; } 
    if (superclass == null) { return res; } 
    return superclass.hasFeature(f); 
  }  

  public boolean hasFeatureOrOperation(String f)
  { boolean res = hasAttribute(f); 
    if (res == true) { return res; } 
    res = hasRole(f); 
    if (res == true) { return res; } 
    BehaviouralFeature op = getOperation(f); 
    if (op != null) 
    { return true; } 
    if (superclass == null) { return res; } 
    return superclass.hasFeatureOrOperation(f); 
  }  

  public Association getRole(String rle)
  { if (rle == null)
    { return null; }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String role2 = ast.getRole2();
      if (rle.equals(role2))
      { return ast; }
    }
    return null;
  }

  public Association getDefinedRole(String rle)
  { Association res = getRole(rle); 
    if (res == null && superclass != null)
    { res = superclass.getDefinedRole(rle); }
    return res; 
  } 

  public Type getDefinedFeatureType(String f) 
  { Attribute att = getDefinedAttribute(f); 
    if (att != null) 
    { return att.getType(); } 
    Association ast = getDefinedRole(f); 
    if (ast != null) 
    { Type e2 = new Type(ast.getEntity2()); 
      if (ast.getCard2() == ONE)
      { return e2; }  // and set/sequence if not ONE
      else if (ast.isOrdered())
      { Type seqtype = new Type("Sequence",null); 
        seqtype.setElementType(e2); 
        return seqtype; 
      } 
      else 
      { Type settype = new Type("Set",null);
        settype.setElementType(e2); 
        return settype; 
      } 
    } 
    return null; 
  } 

  public Type getDefinedFeatureElementType(String f) 
  { Attribute att = getDefinedAttribute(f); 
    if (att != null) 
    { Type aet = att.getElementType(); 
      if (aet != null) 
      { return aet; } 
      return att.getType();
    } 

    Association ast = getDefinedRole(f); 
    if (ast != null) 
    { Type e2 = new Type(ast.getEntity2()); 
      return e2;  
    } 
    return null; 
  } 

  public boolean isBidirectionalRole(String rle)
  { Association res = getDefinedRole(rle); 
    if (res == null) 
    { return false; } 
    String role1 = res.getRole1(); 
    if (role1 != null && role1.length() > 0)
    { return true; } 
    return false; 
  } 

  public boolean isOrderedRole(String rle)
  { Association res = getDefinedRole(rle); 
    if (res == null) 
    { return false; } 
    return res.isOrdered(); 
  } 

  public boolean isSortedRole(String rle)
  { Association res = getDefinedRole(rle); 
    if (res == null) 
    { return false; } 
    return res.isSorted(); 
  } 

  // Only data features: 
  public ModelElement getFeature(String f) 
  { Association ast = getRole(f); 
    if (ast != null) 
    { return ast; } 
    Attribute att = getAttribute(f); 
    if (att != null) 
    { return att; }
    if (superclass != null)
    { return superclass.getFeature(f); } // or interface?
    return null; 
  }  // not ops?
 
  // Only data features: 
  public boolean isFrozen(String f) 
  { Association ast = getRole(f); 
    if (ast != null) 
    { return ast.isFrozen(); } 
    Attribute att = getAttribute(f); 
    if (att != null) 
    { return att.isFrozen(); }
    return false; 
  }  // not ops?

  public Type getFeatureType(String f)
  { // if not in this entity, look in superclass
    ModelElement feat = getFeature(f); 
    if (feat != null) 
    { if (feat instanceof Attribute)
      { return ((Attribute) feat).getType(); } 
      if (feat instanceof Association)
      { Entity e = ((Association) feat).getEntity2(); 
        return new Type(e);
      }   // actually the element type if multiple. 
    }
    return null; 
  }

  public int roleMultiplicity(String rle)
  { if (rle == null)
    { return ONE; }  // ?? 
    Association ast = getRole(rle); 
    if (ast != null) 
    { return ast.getCard2(); }
    return ONE; 
  }  // attributes can now be MANY

  public Attribute getAttribute(String nme) 
  { return (Attribute) ModelElement.lookupByName(nme,attributes); } 

  public Attribute getDefinedAttribute(String nme) 
  { Attribute res = getAttribute(nme); 
    if (res == null && superclass != null)
    { res = superclass.getDefinedAttribute(nme); }
    return res; 
  }  

  public Entity getAttributeOwner(String nme) 
  { Attribute x = getAttribute(nme); 
    if (x == null && superclass != null)
    { return superclass.getAttributeOwner(nme); }
    return this; 
  }  

  public boolean hasOperation(String nme)
  { for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(i); 
      if (nme.equals(bf.getName())) { return true; } 
    } 
    return false; 
  } 

  public boolean hasConcreteOperation(String nme)
  { for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(i); 
      if ( nme.equals(bf.getName()) && !(bf.isAbstract()) ) { return true; } 
    } 
    return false; 
  } 

  public BehaviouralFeature getOperation(String nme) 
  { return (BehaviouralFeature) ModelElement.lookupByName(nme,operations); } 

  public BehaviouralFeature getDefinedOperation(String nme)
  { BehaviouralFeature res = getOperation(nme); 
    if (res != null) 
    { return res; } 
    if (superclass != null) 
    { return superclass.getDefinedOperation(nme); } 
    return null; 
  } 

  public BehaviouralFeature getDefinedOperation(String nme, Vector parameters)
  { BehaviouralFeature res = null; 
    for (int i = 0; i < operations.size(); i++) 
    { res = (BehaviouralFeature) operations.get(i); 
      if (nme.equals(res.getName()) && res.parametersMatch(parameters))
      { return res; } 
    }  

    if (superclass != null) 
    { return superclass.getDefinedOperation(nme,parameters); } 

    return null; 
  } 

  public boolean isClassScope(String nme) 
  { Attribute att = (Attribute) ModelElement.lookupByName(nme,attributes);
    if (att != null) { return att.isClassScope(); }
    Association role = getRole(nme); 
    if (role != null) { return role.isClassScope(); } 
    BehaviouralFeature bf = getOperation(nme); 
    if (bf != null) { return bf.isClassScope(); } 
    return false;  
  } 

  public boolean isExternalApplication()
  { return hasStereotype("externalApp"); } 

  public Vector allFeatures()
  { Vector res = new Vector();
    for (int i = 0; i < attributes.size(); i++)
    { Attribute at = (Attribute) attributes.get(i);
      res.add(at.getName()); 
    }
    for (int j = 0; j < associations.size(); j++) 
    { Association ast = (Association) associations.get(j); 
      res.add(ast.getRole2()); 
    } 
    return res;
  }  // and operation names? 

  public Vector allDefinedFeatures()
  { Vector res = allFeatures(); 
    if (superclass == null)
    { return res; } 
    else 
    { res.addAll(superclass.allDefinedFeatures()); }  
    return res;
  }  // and operation names? 

  public Vector allActIntFeatures()
  { Vector res = new Vector();
    for (int i = 0; i < attributes.size(); i++)
    { Attribute at = (Attribute) attributes.get(i);
      if (at.getKind() == SEN) { }  // and DER?
      else
      { res.add(at.getName()); }
    }
    return res;
  }

  public Vector allSenFeatures()
  { Vector res = new Vector();
    for (int i = 0; i < attributes.size(); i++)
    { Attribute at = (Attribute) attributes.get(i);
      if (at.getKind() == SEN)
      { res.add(at.getName()); }
    }
    return res;
  }

  public Vector allActIntValues()
  { Vector res = new Vector();
    
    for (int i = 0; i < attributes.size(); i++)
    { Attribute at = (Attribute) attributes.get(i);
      if (at.getKind() == SEN) { }  // and DER?
      else
      { Type t = at.getType();
        Vector vals = new Vector(); 
        if (t.getName().equals("boolean"))
        { vals.add("true"); 
          vals.add("false"); 
        } 
        else 
        { vals = t.getValues(); } 
        if (vals == null) { } 
        else 
        { res = VectorUtil.union(res,vals); } 
      }
    }
    return res;
  }

  public Vector allSenValues()
  { Vector res = new Vector();
    
    for (int i = 0; i < attributes.size(); i++)
    { Attribute at = (Attribute) attributes.get(i);
      if (at.getKind() == SEN) 
      { Type t = at.getType();
        Vector vals = new Vector(); 
        if ("boolean".equals(t.getName()))
        { vals.add("true"); 
          vals.add("false"); 
        } 
        else 
        { vals = t.getValues(); } 
        if (vals == null) { } 
        else 
        { res = VectorUtil.union(res,vals); } 
      }
    }
    return res;
  }

  public int compareTo(Object obj)
  { if (obj instanceof Entity)
    { Entity e2 = (Entity) obj;
      return getName().compareTo(e2.getName());
    }
    else // throw exception really
    { System.err.println("Error: can't compare " + this +
                         " and " + obj);
      return 0;
    }
  }
 
  public void addAssociation(Association ast)
  { if (isInterface())
    { System.err.println("WARNING: interfaces cannot have associations from them"); 
      // return; 
    }

    Association oldast = getRole(ast.getRole2()); 
    if (oldast != null)
    { System.err.println("WARNING: Entity " + this + " already has role " + ast.getRole2()); 
      associations.remove(oldast);  

      if (oldast.getCard2() == ModelElement.ONE) 
      { ast.setCard2(ModelElement.ONE); } 
      else if (oldast.getCard2() == ModelElement.ZEROONE && 
             ast.getCard2() == ModelElement.MANY) 
      { ast.setCard2(oldast.getCard2()); } 

      if (oldast.getCard1() == ModelElement.ONE) 
      { ast.setCard1(ModelElement.ONE); } 
      else if (oldast.getCard1() == ModelElement.ZEROONE && 
             ast.getCard1() == ModelElement.MANY) 
      { ast.setCard1(oldast.getCard1()); } 
    } 
    associations.add(ast);

    // if oldast.card2 more specific than ast.card2, use it: 
  }

  public void removeAssociation(Association ast)
  { associations.remove(ast); } 

  public void addContrapositives(Vector entities, Vector types)
  { Vector res = new Vector(); 
    for (int i = 0; i < invariants.size(); i++) 
    { Constraint con = (Constraint) invariants.get(i); 
      if (con.getEvent() != null) { continue; } 
      SafetyInvariant inv = new SafetyInvariant(con.antecedent(),
                                                con.succedent()); 
      Vector contras = SafetyInvariant.genAllContrapositives(inv); 
      for (int j = 0; j < contras.size(); j++) 
      { SafetyInvariant newinv = (SafetyInvariant) contras.get(j); 
        System.out.println("New local invariant: " + newinv); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean tc = newinv.typeCheck(types,entities,contexts,new Vector()); 
        // calculate modality
        Constraint newcon = new Constraint(newinv,con.getAssociations()); 
        if (invariants.contains(newcon)) { } 
        else 
        { newcon.setLocal(con.isLocal()); 
          newcon.setOwner(con.getOwner()); 
          res.add(newcon); 
        } 
      } 
    }
    invariants.addAll(res); 
  }

  public void addTranscomps(Vector entities, Vector types)
  { Vector res = new Vector(); 
    for (int i = 0; i < invariants.size(); i++) 
    { Constraint inv = (Constraint) invariants.get(i); 
      for (int j = i+1; j < invariants.size(); j++) 
      { Constraint inv2 = (Constraint) invariants.get(j);
        if (inv2.getEvent() != null) { continue; } // get next j
        SafetyInvariant sinv = new SafetyInvariant(inv.antecedent(), 
                                                   inv.succedent()); 
        SafetyInvariant sinv2 = new SafetyInvariant(inv2.antecedent(), 
                                                    inv2.succedent());  
        SafetyInvariant newinv = SafetyInvariant.transitiveComp2(sinv,sinv2); 
        if (newinv == null)
        { newinv = SafetyInvariant.transitiveComp3(sinv,sinv2); } 
        System.out.println("New local invariant: " + newinv); 
        if (newinv != null && !newinv.isTrivial())
        { Vector contexts = new Vector(); 
          contexts.add(this); 
          newinv.typeCheck(types,entities,contexts,new Vector());
          Vector ass1 = inv.getAssociations(); 
          Vector newass = VectorUtil.union(ass1,inv2.getAssociations());  
          Constraint newcon = new Constraint(newinv,newass); 
          if (invariants.contains(newcon)) { } 
          else 
          { newcon.setLocal(true); 
            newcon.setOwner(this); 
            res.add(newcon); 
          } 
        }
      } 
    }
    invariants.addAll(res); 
  }

  public void generateIndexes(PrintWriter out)
  { // adds variable Map eattindex for each unique att of entity
    boolean found = false; 

    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isUnique() || uniqueConstraint(att.getName()))
      { String ind = getName().toLowerCase() + att.getName() + "index"; 
        ind = "  Map " + ind + " = new HashMap(); // "; 
        Type t = att.getType(); 
        ind = ind + t.getName() + " --> " + getName() + "\n"; 
        out.println(ind); 
        found = true; 
      } 
    }

    if (!found) // try to build an index for a superclass attribute which is primary here
    { Vector allatts = allInheritedAttributes(); 
      for (int j = 0; j < allatts.size(); j++) 
      { Attribute att1 = (Attribute) allatts.get(j);
        if (att1.isUnique()) { } // an index already exists
        else if (uniqueConstraint(att1.getName())) 
        { String ind = getName().toLowerCase() + att1.getName() + "index"; 
          ind = "  Map " + ind + " = new HashMap(); // "; 
          Type t = att1.getType(); 
          ind = ind + t.getName() + " --> " + getName() + "\n"; 
          out.println(ind);
        }
      }  
    } 
 
  } 


  public void generateIndexesJava7(PrintWriter out)
  { // adds variable Map eattindex for each unique att of entity
    boolean found = false; 
    String nme = getName(); 

    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isUnique() || uniqueConstraint(att.getName()))
      { String ind = getName().toLowerCase() + att.getName() + "index"; 
        ind = "  Map<String, " + nme + "> " + ind + " = new HashMap<String, " + nme + ">(); // "; 
        Type t = att.getType(); 
        ind = ind + t.getName() + " --> " + nme + "\n"; 
        out.println(ind); 
        found = true; 
      } 
    }

    if (!found) // try to build an index for a superclass attribute which is primary here
    { Vector allatts = allInheritedAttributes(); 
      for (int j = 0; j < allatts.size(); j++) 
      { Attribute att1 = (Attribute) allatts.get(j);
        if (att1.isUnique()) { } // an index already exists
        else if (uniqueConstraint(att1.getName())) 
        { String ind = getName().toLowerCase() + att1.getName() + "index"; 
          ind = "  Map<String, " + nme + "> " + ind + " = new HashMap<String, " + nme + ">(); // "; 
          Type t = att1.getType(); 
          ind = ind + t.getName() + " --> " + nme + "\n"; 
          out.println(ind);
        }
      }  
    } 
 
  } 

  public void generateCSharpIndexes(PrintWriter out)
  { // adds variable Map eattindex for each unique att of entity
    boolean found = false; 

    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isUnique() || uniqueConstraint(att.getName()))
      { String ind = getName().toLowerCase() + att.getName() + "index"; 
        ind = "  Hashtable " + ind + " = new Hashtable(); // "; 
        Type t = att.getType(); 
        ind = ind + t.getName() + " --> " + getName() + "\n"; 
        out.println(ind); 
        found = true; 
      } 
    }

    if (!found) // try to build an index for a superclass attribute which is primary here
    { Vector allatts = allInheritedAttributes(); 
      for (int j = 0; j < allatts.size(); j++) 
      { Attribute att1 = (Attribute) allatts.get(j);
        if (att1.isUnique()) { } // an index already exists
        else if (uniqueConstraint(att1.getName())) 
        { String ind = getName().toLowerCase() + att1.getName() + "index"; 
          ind = "  Hashtable " + ind + " = new Hashtable(); // "; 
          Type t = att1.getType(); 
          ind = ind + t.getName() + " --> " + getName() + "\n"; 
          out.println(ind);
        }
      }  
    } 

  } 

  public void staticAttributeDefinitions(PrintWriter out)
  { // for C++
    String ename = getName(); 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isStatic())
      { att.staticAttributeDefinition(out, ename); } 
    } 
  } 

  public void generateIndexesCPP(PrintWriter out)
  { // adds variable Map eattindex for each unique att of entity
    boolean found = false; 

    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isUnique() || uniqueConstraint(att.getName()))
      { String ind = getName().toLowerCase() + att.getName() + "index"; 
        Type t = att.getType();
        String cppt = t.getCPP();  
        ind = "  map<" + cppt + "," + getName() + "*> " + ind + ";"; 
        out.println(ind); 
        found = true; 
      } 
    }

    if (!found) // try to build an index for a superclass attribute which is primary here
    { Vector allatts = allInheritedAttributes(); 
      for (int j = 0; j < allatts.size(); j++) 
      { Attribute att1 = (Attribute) allatts.get(j);
        if (att1.isUnique()) { } // an index already exists
        else if (uniqueConstraint(att1.getName())) 
        { String ind = getName().toLowerCase() + att1.getName() + "index"; 
          Type t = att1.getType();
          String cppt = t.getCPP();  
          ind = "  map<" + cppt + "," + getName() + "*> " + ind + ";"; 
          out.println(ind);
        }
      }  
    } 
 
  } 


  public String controllerIndexCode(Attribute att, String ex)
  { String ind = getName().toLowerCase() + att.getName() + "index";
    String attval = ex + ".get" + att.getName() + "()"; 
    return "  Controller.inst()." + ind + ".put(" + attval + ", " + ex + ")"; 
  } 

  // Primary keys must be of String type.
  public String generateIndexOp()
  { // adds operation getEByPK for first unique att of entity
    String res = ""; 
    String nme = getName(); 
    String ex = nme.toLowerCase() + "x"; 
    
    Attribute att = getPrincipalPK(); 
    if (att != null)
    { String attentnme = att.getEntity() + ""; 
      String ind = attentnme.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getName(); 
      String attx = att.getName() + "x"; 
      String test = ""; 
      if (this != att.getEntity())  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + ".get(" + attx + ") instanceof " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " +
                    attx + ")\n" + 
                    "  {" + test + "  return (" + nme + ") " + ind + ".get(" + attx + "); }\n\n" +

              "  public List get" + nme + "ByPK(List " + attx + ")\n" + 
              "  { Vector res = new Vector(); \n" + 
              "    for (int _i = 0; _i < " + attx + ".size(); _i++)\n" + 
              "    { " + nme + " " + ex + " = get" + nme + "ByPK((" + tname + ") " + attx + ".get(_i));\n" + 
              "      if (" + ex + " != null) { res.add(" + ex + "); }\n" + 
              "    }\n" + 
              "    return res; \n" + 
              "  }\n\n"; 
      return res; 
    } 

    Vector decEnt = new Vector(); 
    att = getPrincipalUK(this, decEnt); 
    // System.out.println("====>>> " + att + " " + decEnt); 
   
    if (att != null && decEnt.size() > 0) 
    { Entity decE = (Entity) decEnt.get(0); 
      String sname = decE.getName(); 
      String ind = sname.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getName(); 
      String attx = att.getName() + "x"; 
      String test = ""; 
      if (this != decE)  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + ".get(" + attx + ") instanceof " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " +
                    attx + ")\n" + 
                    "  {" + test +   
                    "   return (" + nme + ") " + ind + ".get(" + attx + "); }\n\n" +

              "  public List get" + nme + "ByPK(List " + attx + ")\n" + 
              "  { Vector res = new Vector(); \n" + 
              "    for (int _i = 0; _i < " + attx + ".size(); _i++)\n" + 
              "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") " + attx + ".get(_i));\n" + 
              "      if (" + nme + "x != null) { res.add(" + nme + "x); }\n" + 
              "    }\n" + 
              "    return res; \n" + 
              "  }\n\n"; 
      return res; 
    } 

    return res; 
  } 
  // getAll, and for any superclass primary key, the filtered version of this

  public String generateIndexOpJava6()
  { // adds operations getEByPK for first unique att of entity: assumes this is of String type
    String res = ""; 
    String nme = getName(); 
    String ex = nme.toLowerCase() + "_x"; 
    
    Attribute att = getPrincipalPK(); 
    if (att != null)
    { String attentnme = att.getEntity() + ""; 
      String ind = attentnme.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getName(); 
      String attx = att.getName() + "_x"; 
      String test = ""; 
      if (this != att.getEntity())  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + ".get(" + attx + ") instanceof " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " + attx + ")\n" + 
            "  {" + test + "  return (" + nme + ") " + ind + ".get(" + attx + "); }\n\n" +

            "  public HashSet get" + nme + "ByPK(HashSet " + attx + ")\n" + 
            "  { HashSet res = new HashSet(); \n" + 
            "    for (Object _o : " + attx + ")\n" + 
            "    { " + nme + " " + ex + " = get" + nme + "ByPK((" + tname + ") _o);\n" + 
            "      if (" + ex + " != null) { res.add(" + ex + "); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n" + 

            "  public ArrayList get" + nme + "ByPK(ArrayList " + attx + ")\n" + 
            "  { ArrayList res = new ArrayList(); \n" + 
            "    for (int _i = 0; _i < " + attx + ".size(); _i++)\n" + 
            "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") " + attx + ".get(_i));\n" + 
            "      if (" + nme + "x != null) { res.add(" + nme + "x); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n"; 
 
      return res; 
    } 

    Vector decEnt = new Vector(); 
    att = getPrincipalUK(this, decEnt); 
    // System.out.println("====>>> " + att + " " + decEnt); 
   
    if (att != null && decEnt.size() > 0) 
    { Entity decE = (Entity) decEnt.get(0); 
      String sname = decE.getName(); 
      String ind = sname.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getName(); // String
      String attx = att.getName() + "_x"; 
      String test = ""; 
      if (this != decE)  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + ".get(" + attx + ") instanceof " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " + attx + ")\n" + 
            "  {" + test +   
            "   return (" + nme + ") " + ind + ".get(" + attx + "); }\n\n" +

            "  public HashSet get" + nme + "ByPK(HashSet " + attx + ")\n" + 
            "  { HashSet res = new HashSet(); \n" + 
            "    for (Object _o : " + attx + ")\n" + 
            "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") _o);\n" + 
            "      if (" + nme + "x != null) { res.add(" + nme + "x); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n" +
 
            "  public ArrayList get" + nme + "ByPK(ArrayList " + attx + ")\n" + 
            "  { ArrayList res = new ArrayList(); \n" + 
            "    for (int _i = 0; _i < " + attx + ".size(); _i++)\n" + 
            "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") " + attx + ".get(_i));\n" + 
            "      if (" + nme + "x != null) { res.add(" + nme + "x); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n"; 
      return res; 
    } 

    return res; 
  } 
  // getAll, and for any superclass primary key, the filtered version of this


  public String generateIndexOpJava7()
  { // adds operations getEByPK for first unique att of entity
    String res = ""; 
    String nme = getName(); 
    String ex = nme.toLowerCase() + "_x"; 
    
    Attribute att = getPrincipalPK(); 
    if (att != null)
    { String attentnme = att.getEntity() + ""; 
      String ind = attentnme.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      // String jtype = t.getJava7(att.getElementType()); 
      String tname = t.getName(); 
      String attx = att.getName() + "_x"; 
      String test = ""; 
      if (this != att.getEntity())  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + ".get(" + attx + ") instanceof " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " + attx + ")\n" + 
            "  {" + test + "  return (" + nme + ") " + ind + ".get(" + attx + "); }\n\n" +

            "  public HashSet<" + nme + "> get" + nme + "ByPK(HashSet<String> " + attx + ")\n" + 
            "  { HashSet<" + nme + "> res = new HashSet<" + nme + ">(); \n" + 
            "    for (Object _o : " + attx + ")\n" + 
            "    { " + nme + " " + ex + " = get" + nme + "ByPK((" + tname + ") _o);\n" + 
            "      if (" + ex + " != null) { res.add(" + ex + "); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n" + 

            "  public ArrayList<" + nme + "> get" + nme + "ByPK(ArrayList<String> " + attx + ")\n" + 
            "  { ArrayList<" + nme + "> res = new ArrayList<" + nme + ">(); \n" + 
            "    for (int _i = 0; _i < " + attx + ".size(); _i++)\n" + 
            "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") " + attx + ".get(_i));\n" + 
            "      if (" + nme + "x != null) { res.add(" + nme + "x); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n"; 
 
      return res; 
    } 

    Vector decEnt = new Vector(); 
    att = getPrincipalUK(this, decEnt); 
    // System.out.println("====>>> " + att + " " + decEnt); 
   
    if (att != null && decEnt.size() > 0) 
    { Entity decE = (Entity) decEnt.get(0); 
      String sname = decE.getName(); 
      String ind = sname.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getName(); 
      String attx = att.getName() + "_x"; 
      String test = ""; 
      if (this != decE)  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + ".get(" + attx + ") instanceof " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " + attx + ")\n" + 
            "  {" + test +   
            "   return (" + nme + ") " + ind + ".get(" + attx + "); }\n\n" +

            "  public HashSet<" + nme + "> get" + nme + "ByPK(HashSet<String> " + attx + ")\n" + 
            "  { HashSet<" + nme + "> res = new HashSet<" + nme + ">(); \n" + 
            "    for (Object _o : " + attx + ")\n" + 
            "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") _o);\n" + 
            "      if (" + nme + "x != null) { res.add(" + nme + "x); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n" +
 
            "  public ArrayList<" + nme + "> get" + nme + "ByPK(ArrayList<String> " + attx + ")\n" + 
            "  { ArrayList<" + nme + "> res = new ArrayList<" + nme + ">(); \n" + 
            "    for (int _i = 0; _i < " + attx + ".size(); _i++)\n" + 
            "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") " + attx + ".get(_i));\n" + 
            "      if (" + nme + "x != null) { res.add(" + nme + "x); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n"; 
      return res; 
    } 

    return res; 
  } 
  // getAll, and for any superclass primary key, the filtered version of this

  public String generateCSharpIndexOp()
  { // adds operation getEByPK for first unique att of entity
    String res = ""; 
    String nme = getName(); 
    String ex = nme.toLowerCase() + "x"; 
    
    Attribute att = getPrincipalPK(); 
    if (att != null)
    { String attentnme = att.getEntity() + ""; 
      String ind = attentnme.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getCSharp(); 
      String attx = att.getName() + "x"; 
      String test = ""; 
      if (this != att.getEntity())  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + "[" + attx + "] is " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " +
                    attx + ")\n" + 
                    "  {" + test + "  return (" + nme + ") " + ind + "[" + attx + "]; }\n\n" +

              "  public ArrayList get" + nme + "ByPK(ArrayList " + attx + ")\n" + 
              "  { ArrayList res = new ArrayList(); \n" + 
              "    for (int _i = 0; _i < " + attx + ".Count; _i++)\n" + 
              "    { " + nme + " " + ex + " = get" + nme + "ByPK((" + tname + ") " + attx + "[_i]);\n" + 
              "      if (" + ex + " != null) { res.Add(" + ex + "); }\n" + 
              "    }\n" + 
              "    return res; \n" + 
              "  }\n\n"; 
      return res; 
    } 

    Vector decEnt = new Vector(); 
    att = getPrincipalUK(this, decEnt); 
    // System.out.println("====>>> " + att + " " + decEnt); 
   
    if (att != null && decEnt.size() > 0) 
    { Entity decE = (Entity) decEnt.get(0); 
      String sname = decE.getName(); 
      String ind = sname.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getCSharp(); 
      String attx = att.getName() + "_x"; 
      String test = ""; 
      if (this != decE)  // The PK is of a strict superclass
      { test = 
          " if (!(" + ind + "[" + attx + "] is " + nme + ")) { return null; }\n";
      } 
      res = "  public " + nme + " get" + nme + "ByPK(" + tname + " " + attx + ")\n" + 
            "  {" + test +   
            "   return (" + nme + ") " + ind + "[" + attx + "]; }\n\n" +

            "  public ArrayList get" + nme + "ByPK(ArrayList " + attx + ")\n" + 
            "  { ArrayList res = new ArrayList(); \n" + 
            "    for (int _i = 0; _i < " + attx + ".Count; _i++)\n" + 
            "    { " + nme + " " + nme + "x = get" + nme + "ByPK((" + tname + ") " + attx + "[_i]);\n" + 
            "      if (" + nme + "x != null) { res.Add(" + nme + "x); }\n" + 
            "    }\n" + 
            "    return res; \n" + 
            "  }\n\n"; 
      return res; 
    } 

    return res; 
  } 

  public String generateIndexOpCPP()
  { // adds operation getEByPK for first unique att of entity
    String res = ""; 
    String nme = getName(); 
    String ex = nme.toLowerCase() + "x"; 
    
    Attribute att = getPrincipalPK(); 
    if (att != null)
    { String attentnme = att.getEntity() + ""; 
      String ind = attentnme.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getCPP(); 
      String attx = att.getName() + "x"; 
      String test = "  if (" + ind + ".find(" + attx + ") == " + ind + ".end()) { return 0; }\n"; 

      String cast = ""; 
      if (this != att.getEntity())  // The PK is of a strict superclass
      { cast = "(" + nme + "*) "; }  

      res = "  " + nme + "* get" + nme + "ByPK(" + tname + " " + attx + ")\n" + 
                    "  {" + test + "  return " + cast + ind + "[" + attx + "]; }\n\n" +

              "  vector<" + nme + "*>* get" + nme + "ByPK(vector<" + tname + ">* " + attx + ")\n" + 
              "  { vector<" + nme + "*>* res = new vector<" + nme + "*>(); \n" + 
              "    for (int _i = 0; _i < " + attx + "->size(); _i++)\n" + 
              "    { " + nme + "* " + ex + " = get" + nme + "ByPK((*" + attx + ")[_i]);\n" + 
              "      if (" + ex + " != 0) { res->push_back(" + ex + "); }\n" + 
              "    }\n" + 
              "    return res; \n" + 
              "  }\n\n" +
              "  set<" + nme + "*>* get" + nme + "ByPK(set<" + tname + ">* " + attx + ")\n" + 
              "  { set<" + nme + "*>* res = new set<" + nme + "*>(); \n" + 
              "    set<" + tname + ">::iterator _pos; \n" + 
              "    for (_pos = " + attx + "->begin(); _pos != " + attx + "->end(); ++_pos)\n" + 
              "    { " + nme + "* " + ex + " = get" + nme + "ByPK(*_pos);\n" + 
              "      if (" + ex + " != 0) { res->insert(" + ex + "); }\n" + 
              "    }\n" + 
              "    return res; \n" + 
              "  }\n\n"; 
 
      return res; 
    } 

    Vector decEnt = new Vector(); 
    att = getPrincipalUK(this, decEnt); 
    // System.out.println("====>>> " + att + " " + decEnt); 
   
    if (att != null && decEnt.size() > 0) 
    { Entity decE = (Entity) decEnt.get(0); 
      String sname = decE.getName(); 
      String ind = sname.toLowerCase() + att.getName() + "index"; 
      Type t = att.getType(); 
      String tname = t.getCPP(); 
      String attx = att.getName() + "_x"; 
      String test = "  if (" + ind + ".find(" + attx + ") == " + ind + ".end()) { return 0; }\n"; 

      String cast = ""; 
      if (this != decE)  // The PK is of a strict superclass
      { cast = "(" + nme + "*) "; }  

      res = "  " + nme + "* get" + nme + "ByPK(" + tname + " " + attx + ")\n" + 
                    "  {" + test + "  return " + cast + ind + "[" + attx + "]; }\n\n" +

              "  vector<" + nme + "*>* get" + nme + "ByPK(vector<" + tname + ">* " + attx + ")\n" + 
              "  { vector<" + nme + "*>* res = new vector<" + nme + "*>(); \n" + 
              "    for (int _i = 0; _i < " + attx + "->size(); _i++)\n" + 
              "    { " + nme + "* " + ex + " = get" + nme + "ByPK((*" + attx + ")[_i]);\n" + 
              "      if (" + ex + " != 0) { res->push_back(" + ex + "); }\n" + 
              "    }\n" + 
              "    return res; \n" + 
              "  }\n\n" +
              "  set<" + nme + "*>* get" + nme + "ByPK(set<" + tname + ">* " + attx + ")\n" + 
              "  { set<" + nme + "*>* res = new set<" + nme + "*>(); \n" + 
              "    set<" + tname + ">::iterator _pos; \n" + 
              "    for (_pos = " + attx + "->begin(); _pos != " + attx + "->end(); ++_pos)\n" + 
              "    { " + nme + "* " + ex + " = get" + nme + "ByPK(*_pos);\n" + 
              "      if (" + ex + " != 0) { res->insert(" + ex + "); }\n" + 
              "    }\n" + 
              "    return res; \n" + 
              "  }\n\n"; 
      return res; 
    } 

    return res; 
  } 

  public Attribute getPrincipalPK()
  { // first unique att of entity
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isUnique())
      { return att; }
    } 

    if (superclass != null) 
    { return superclass.getPrincipalPK(); } 

    return null; 
  } 

  /* public Attribute getSuperPrincipalPK()
  { // first unique att of entity
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isUnique())
      { return att; }
    } 
    if (superclass != null) 
    { return superclass.getSuperPrincipalPK(); } 
    return null; 
  } */ 
 
  public String toCSV()
  { String res1 = "" + getName() + ":\n";
    String res2 = "";  
    for (int i = 0; i < attributes.size(); i++)  // all attributes in fact
    { Attribute att = (Attribute) attributes.get(i); 
      String nme = att.getName(); 
      Type t = att.getType(); 
      String ini = att.getInitialValue(); 
      res1 = res1 + nme + "; "; 
      if (t != null && "double".equals(t.getName()))
      { res2 = res2 + "0.0; "; } 
      else 
      { res2 = res2 + ini + "; "; }  
    } 
    return res1 + "\n" + res2 + "\n\n"; 
  } 

  public String parseCSVOperation()
  { if (isAbstract()) { return ""; } 

    String ename = getName(); 
    String ex = ename.toLowerCase() + "x"; 

    String res = 
      "  public static " + ename + " parseCSV(String _line)\n" + 
      "  { if (_line == null) { return null; }\n" +   
      "    Vector _line1vals = Set.tokeniseCSV(_line);\n" + 
      "    " + ename + " " + ex + " = new " + ename + "();\n"; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String aname = att.getName(); 
      Type atype = att.getType(); 
      String data = "(String) _line1vals.get(" + i + ")"; 

      if (atype.isParsable())
      { res = res + 
          "    " + ex + "." + aname + " = " + atype.dataExtractionCode(data) + ";\n"; 
      }  
      else if (atype.isCollectionType())
      { Type elemT = atype.getElementType(); 
        if (elemT != null && elemT.isParsable())
        res = res + "    " + atype.collectionExtractionCode(ex,aname,"_line1vals") + ";\n"; 
      }          
      if (att.isUnique())
      { res = res + "    " + controllerIndexCode(att,ex) + ";\n"; } 
    }  
    res = res + 
      "    return " + ex + ";\n" + 
      "  }\n\n"; 
    return res; 
  } 

  public String writeCSVOperation()
  { if (isAbstract()) { return ""; } 

    String ename = getName(); 
    String ex = ename.toLowerCase() + "x"; 

    String res = 
      "  public void writeCSV(PrintWriter _out)\n" + 
      "  { " + ename + " " + ex + " = this;\n"; 
    int n = attributes.size(); 
    for (int i = 0; i < n; i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String aname = att.getName(); 

      res = res + 
        "    _out.print(\"\" + " + ex + "." + aname + ");\n";
      if (i < n-1) 
      { res = res + 
          "    _out.print(\" , \");\n";
      } 
    }  
    res = res + 
      "    _out.println();\n" + 
      "  }\n\n"; 
    return res; 
  } 

  public Attribute getPrincipalUK(Entity sub, Vector declaredIn)
  { for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 

      // System.out.println("=====>>> " + att); 

      if (sub.uniqueConstraint(att.getName()))
      { declaredIn.add(sub); 
        return att; 
      }
    } 
    Attribute res = null; 

    if (superclass != null) 
    { res = superclass.getPrincipalUK(this,declaredIn); } 
    else 
    { return null; }

    if (res == null) 
    { return superclass.getPrincipalUK(superclass,declaredIn); } 
    return res; 
  } 

  public Attribute getPrincipalKey()
  { // Either the primary key or a declared unique key: 

    Attribute res = getPrincipalPK(); 
    if (res == null) 
    { Vector decEnt = new Vector(); 
      res = getPrincipalUK(this, decEnt); 
    } 
    if (res == null) 
    { JOptionPane.showMessageDialog(null, "ERROR: No key attribute for " + getName(), 
                             "Error in object lookup expression!",
                             JOptionPane.ERROR_MESSAGE);  
    }   // a bad error      
    return res; 
  } 
 
  public void generateJava(PrintWriter out)
  { generateJava(new Vector(), new Vector(),out); } 
  // never called

  public void generateCode(String language, Vector entities, Vector types, PrintWriter out,
                           PrintWriter out2)
  { if ("Java4".equals(language))
    { generateJava(entities,types,out); } 
    else if ("Java6".equals(language))
    { generateJava6(entities,types,out); } 
    else if ("Java7".equals(language))
    { generateJava7(entities,types,out); } 
    else if ("CSharp".equals(language))
    { generateCSharp(entities,types,out); } 
    else 
    { generateCPP(entities,types,out,out2); } 
  } 
 
  public void generateJava(Vector entities, Vector types, PrintWriter out)
  { if (hasStereotype("external") || hasStereotype("externalApp")) { return; } 

    clearAux(); 

    String intorclass = "class"; 
    if (isInterface())
    { intorclass = "interface"; } 
    else if (isAbstract())
    { out.print("abstract "); }
    else if (isLeaf())
    { out.print("final "); }
    out.println(intorclass + " " + getName()); 
    if (superclass != null) 
    { out.println("  extends " + superclass.getName()); } 
    
    if (isInterface())
    { out.print("  extends SystemTypes"); } 
    else 
    { out.print("  implements SystemTypes"); }

    for (int j = 0; j < interfaces.size(); j++)
    { Entity intf = (Entity) interfaces.get(j); 
      String iname = intf.getName(); 
      out.print(", " + iname); 
    }

    if (isActive())
    { out.print(", Runnable"); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { addRunStates(bf.getSm()); } 
    } 
    out.println(); 
    out.println("{");

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (isInterface())
      { att.generateInterfaceJava(out); } 
      else 
      { att.generateJava(out); } 
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      if (isInterface())
      { ast.generateInterfaceJava(out); } 
      else 
      { ast.generateJava(out); } 
    }


    /* if (linkedAssociation != null) 
    { // an att for each end of it
      Entity ent1 = linkedAssociation.getEntity1(); 
      Entity ent2 = linkedAssociation.getEntity2(); 
      String e1name = ent1.getName(); 
      String e2name = ent2.getName(); 
      String e1att = e1name.toLowerCase(); // just 1st letter, really
      String e2att = e2name.toLowerCase(); 
      out.println("  private " + e1name + " " + e1att + ";"); 
      out.println("  private " + e2name + " " + e2att + ";"); 
    } */ 

    out.println();
    buildConstructor(out, entities, types);
    out.println("\n");

    BehaviouralFeature ttt = getOperation("toString"); 
    if (ttt == null)
    { String tosop = generateToStringOp(); 
      out.println(tosop + "\n"); 
    }

    out.println(parseCSVOperation()); 

    out.println(writeCSVOperation()); 

    buildOperations(entities,types,out);
    // if (activity != null)
    // { buildMainOperation(entities,types,out); } 

    out.println(auxiliaryElements); // Maps for each cached operation

    out.println("}\n");
  }

  public void generateJava6(Vector entities, Vector types, PrintWriter out)
  { if (hasStereotype("external") || hasStereotype("externalApp")) { return; } 

    clearAux(); 

    String intorclass = "class"; 
    if (isInterface())
    { intorclass = "interface"; } 
    else if (isAbstract())
    { out.print("abstract "); }
    else if (isLeaf())
    { out.print("final "); }
    out.println(intorclass + " " + getName()); 
    if (superclass != null) 
    { out.println("  extends " + superclass.getName()); } 
    
    if (isInterface())
    { out.print("  extends SystemTypes"); } 
    else 
    { out.print("  implements SystemTypes"); }

    for (int j = 0; j < interfaces.size(); j++)
    { Entity intf = (Entity) interfaces.get(j); 
      String iname = intf.getName(); 
      out.print(", " + iname); 
    }

    if (isActive())
    { out.print(", Runnable"); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { addRunStates(bf.getSm()); } 
    } 
    out.println(); 
    out.println("{");

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (isInterface())
      { att.generateInterfaceJava6(out); } 
      else 
      { att.generateJava6(out); } 
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      if (isInterface())
      { ast.generateInterfaceJava6(out); } 
      else 
      { ast.generateJava6(out); } 
    }

    /* if (linkedAssociation != null) 
    { // an att for each end of it
      Entity ent1 = linkedAssociation.getEntity1(); 
      Entity ent2 = linkedAssociation.getEntity2(); 
      String e1name = ent1.getName(); 
      String e2name = ent2.getName(); 
      String e1att = e1name.toLowerCase(); // just 1st letter, really
      String e2att = e2name.toLowerCase(); 
      out.println("  private " + e1name + " " + e1att + ";"); 
      out.println("  private " + e2name + " " + e2att + ";"); 
    } */ 

    out.println();
    buildConstructorJava6(out);
    out.println("\n");

    BehaviouralFeature ttt = getOperation("toString"); 
    if (ttt == null)
    { String tosop = generateToStringOp(); 
      out.println(tosop + "\n"); 
    }

    buildOperationsJava6(entities,types,out);
    // if (activity != null)
    // { buildMainOperation(entities,types,out); } 

    out.println(auxiliaryElements); // Maps for each cached operation

    out.println("}\n");
  }

  public void generateJava7(Vector entities, Vector types, PrintWriter out)
  { if (hasStereotype("external") || hasStereotype("externalApp")) { return; } 

    clearAux(); 

    String intorclass = "class"; 
    if (isInterface())
    { intorclass = "interface"; } 
    else if (isAbstract())
    { out.print("abstract "); }
    else if (isLeaf())
    { out.print("final "); }
    out.println(intorclass + " " + getName()); 
    if (superclass != null) 
    { out.println("  extends " + superclass.getName()); } 
    
    if (isInterface())
    { out.print("  extends SystemTypes"); } 
    else 
    { out.print("  implements SystemTypes"); }

    for (int j = 0; j < interfaces.size(); j++)
    { Entity intf = (Entity) interfaces.get(j); 
      String iname = intf.getName(); 
      out.print(", " + iname); 
    }

    if (isActive())
    { out.print(", Runnable"); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { addRunStates(bf.getSm()); } 
    } 
    out.println(); 
    out.println("{");

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (isInterface())
      { att.generateInterfaceJava7(out); } 
      else 
      { att.generateJava7(out); } 
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      if (isInterface())
      { ast.generateInterfaceJava7(out); } 
      else 
      { ast.generateJava7(out); } 
    }

    /* if (linkedAssociation != null) 
    { // an att for each end of it
      Entity ent1 = linkedAssociation.getEntity1(); 
      Entity ent2 = linkedAssociation.getEntity2(); 
      String e1name = ent1.getName(); 
      String e2name = ent2.getName(); 
      String e1att = e1name.toLowerCase(); // just 1st letter, really
      String e2att = e2name.toLowerCase(); 
      out.println("  private " + e1name + " " + e1att + ";"); 
      out.println("  private " + e2name + " " + e2att + ";"); 
    } */ 

    out.println();
    buildConstructorJava7(out);
    out.println("\n");

    BehaviouralFeature ttt = getOperation("toString"); 
    if (ttt == null)
    { String tosop = generateToStringOp(); 
      out.println(tosop + "\n"); 
    }

    buildOperationsJava7(entities,types,out);
    // if (activity != null)
    // { buildMainOperation(entities,types,out); } 

    out.println(auxiliaryElements); // Maps for each cached operation

    out.println("}\n");
  }

  public void generateCSharp(Vector entities, Vector types, PrintWriter out)
  { if (hasStereotype("external") || hasStereotype("externalApp")) { return; } 
    String intorclass = "class"; 
    out.println(); 

    clearAux(); 

    if (isInterface())
    { intorclass = "interface"; } 
    else if (isAbstract())
    { out.print("abstract "); }
    else if (isLeaf())
    { out.print("sealed "); }
    out.print(intorclass + " " + getName()); 
    // out.print("  : SystemTypes");
    if (superclass != null) 
    { out.println(" : " + superclass.getName()); } 
    

    for (int j = 0; j < interfaces.size(); j++)
    { Entity intf = (Entity) interfaces.get(j); 
      String iname = intf.getName(); 
      out.print(", " + iname); 
    }

    /* if (isActive())
    { out.print(", Runnable"); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { addRunStates(bf.getSm()); } 
    } */ 
    out.println(); 
    out.println("{");

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (isInterface())
      { att.generateInterfaceCSharp(out); } 
      else 
      { att.generateCSharp(out); } 
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      if (isInterface())
      { ast.generateInterfaceCSharp(out); } 
      else 
      { ast.generateCSharp(out); } 
    }

    out.println();
    buildCSharpConstructor(out);
    out.println("\n");

    BehaviouralFeature ttt = getOperation("ToString"); 
    if (ttt == null)
    { String tosop = generateCSharpToStringOp(); 
      out.println(tosop + "\n"); 
    }

    buildCSharpOperations(entities,types,out);
    // if (activity != null)
    // { buildMainOperation(entities,types,out); } 

    out.println(auxiliaryElements); // Maps for each cached operation

    out.println("}\n");
  }

  public void generateCPP(Vector entities, Vector types, PrintWriter out, PrintWriter out2)
  { if (hasStereotype("external") || hasStereotype("externalApp")) 
    { out2.println("#include \"" + getName() + ".h\""); 
      return; 
    }  // It will be #included in the .cpp

    // out.println("// " + getName() + ".h"); 

    clearAux(); 
    
    String intorclass = "class"; 
    // if (isInterface())
    // { intorclass = "virtual class"; } 
    // else if (isAbstract())
    // { out.print("abstract "); }
    // else if (isLeaf())
    // { out.print("final "); }
    out.print(intorclass + " " + getName()); 

    boolean previous = false; 
    if (superclass != null) // May be several 
    { out.print("  : public " + superclass.getName()); 
      previous = true; 
    } 
    
    // if (isInterface())
    // { out.print("  extends SystemTypes"); } 
    // else 
    // { out.print("  implements SystemTypes"); }

    for (int j = 0; j < interfaces.size(); j++)
    { Entity intf = (Entity) interfaces.get(j); 
      String iname = intf.getName(); 
      if (previous) 
      { out.print(", public " + iname); } 
      else 
      { out.print(" : public " + iname); 
        previous = true; 
      }  
    }

    /* if (isActive())
    { out.print(", Runnable"); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { addRunStates(bf.getSm()); } 
    } */ 

    out.println(); 
    out.println("{ "); 
    if (attributes.size() + associations.size() > 0)
    { if (subclasses.size() > 0)
      { out.println(" protected:"); } 
      else 
      { out.println(" private:"); } 
    } 


    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      att.generateCPP(out); 
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      ast.generateCPP(out); 
    }

    // buildOperationsCPP(entities,types,out,out2);


    out.println();
    out.println("  public:"); 

    buildConstructorCPP(out);
    out.println("\n");

    BehaviouralFeature ttt = getOperation("toString"); 
    if (ttt == null)
    { String tosop = generateToStringOpCPP(); 
      out.println(tosop + "\n"); 
    }

    buildOperationsCPP(entities,types,out,out2);

    // if (activity != null)
    // { buildMainOperation(entities,types,out); } 

    // out.println(privateAux); // Maps for each cached operation

    out.println(auxiliaryElements); 

    buildDestructorCPP(out); 

    out.println("};\n");
  }

  public Vector genQueryOpCode(Vector cons)
  { Vector res = new Vector();
    for (int i = 0; i < operations.size(); i++)
    { BehaviouralFeature f =
        (BehaviouralFeature) operations.get(i);
      if (f.isQuery())
      { res.add(f.genQueryCode(this,cons)); }
    }
    return res;
  }

  public SmvModule generateSmv()
  { if (cardinality == null || cardinality.equals("*"))
    { System.err.println("Cannot convert unbounded cardinality class to SMV"); 
      return null; 
    } 
    int card = 0; 
    try { card = Integer.parseInt(cardinality); }
    catch (Exception e) 
    { System.err.println("Cannot convert unbounded cardinality class to SMV"); 
      return null; 
    }
    SmvModule res = new SmvModule(getName()); 
    if (hasStereotype("source"))
    { res.buildSourceModule(card,attributes,associations,superclass); } 
    else 
    { res.buildModule(card,attributes,associations,superclass); } 
    return res; 
  } 

  public BComponent generateB(Vector entities, Vector types)
  { // BComponent res = new BComponent(getName());
    // res.addSees("SystemTypes");
    // for (int i = 0; i < attributes.size(); i++)
    // { Attribute att = (Attribute) attributes.get(i);
    //   res.addAttribute(att);
    // }
    // for (int i = 0; i < associations.size(); i++)
    // { Association ast = 
    //     (Association) associations.get(i);
    //   String role2 = ast.getRole2();
    //   res.addRole(role2,ast);
    // } 
    // res.buildConstructor();
    // return res;
    if (isInterface())
    { return new BComponent(types,entities,invariants,this); } 
    return new BComponent(this,entities,types,invariants); 
  }
 
  private void buildConstructor(PrintWriter out, Vector entities, Vector types)
  { if (isInterface()) { return; } 
    String nme = getName(); 
    String vis = "public"; 
    if (isSingleton()) 
    { vis = "private";
      String inst = "instance_" + nme; 
      out.println("  public static " + nme + " inst()"); 
      out.println("  { if (" + inst + " == null) { " + 
                           inst + " = new " + nme + "(); }"); 
      out.println("    return " + inst + ";"); 
      out.println("  }\n"); 
    } 

    out.print("  " + vis + " " + nme + "(");
    boolean previous = false;
    String res = "";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.constructorParameter();
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else        
        { res = par;
          previous = true;
        }
      }
    }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String par = ast.constructorParameter();
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else
        { res = par;
          previous = true;
        }
      }
    }
    out.println(res + ")\n  {");
    buildConstructorCode(out, entities, types);
    out.println("  }\n");

    if (previous)  // constructor has arguments
    { out.println("  " + vis + " " + nme + "() { }\n"); } 
  }

  private void buildConstructorCode(PrintWriter out, Vector entities, Vector types)
  { for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String ini = att.initialiser(entities, types);
      if (ini != null)
      { out.println("    " + ini); }
    }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String ini = ast.initialiser();
      if (ini != null)
      { out.println("    " + ini); }
    }

    Vector vars = allDefinedFeatures(); 

    String code = ""; 
    String nme = getName(); 
    java.util.Map env = new java.util.HashMap(); 
    env.put(nme,"this"); 

    for (int i = 0; i < invariants.size(); i++)
    { Constraint cc = (Constraint) invariants.get(i); 
      code = code + cc.updateForm(env,true); 
    } 
    out.println(code); 
  } 

  private void buildConstructorJava6(PrintWriter out)
  { if (isInterface()) { return; } 
    String nme = getName(); 
    String vis = "public"; 
    if (isSingleton()) 
    { vis = "private";
      String inst = "instance_" + nme; 
      out.println("  public static " + nme + " inst()"); 
      out.println("  { if (" + inst + " == null) { " + 
                           inst + " = new " + nme + "(); }"); 
      out.println("    return " + inst + ";"); 
      out.println("  }\n"); 
    } 

    out.print("  " + vis + " " + nme + "(");
    boolean previous = false;
    String res = "";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.constructorParameterJava6();
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else        
        { res = par;
          previous = true;
        }
      }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String par = ast.constructorParameterJava6();
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else
        { res = par;
          previous = true;
        }
      }
    }
    out.println(res + ")\n  {");
    buildConstructorCodeJava6(out);
    out.println("  }\n");

    if (previous)  // constructor has arguments
    { out.println("  " + vis + " " + nme + "() { }\n"); } 
  }

  private void buildConstructorJava7(PrintWriter out)
  { if (isInterface()) { return; } 
    String nme = getName(); 
    String vis = "public"; 
    if (isSingleton()) 
    { vis = "private";
      String inst = "instance_" + nme; 
      out.println("  public static " + nme + " inst()"); 
      out.println("  { if (" + inst + " == null) { " + 
                           inst + " = new " + nme + "(); }"); 
      out.println("    return " + inst + ";"); 
      out.println("  }\n"); 
    } 

    out.print("  " + vis + " " + nme + "(");
    boolean previous = false;
    String res = "";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.constructorParameterJava7();  
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else        
        { res = par;
          previous = true;
        }
      }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String par = ast.constructorParameterJava7();
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else
        { res = par;
          previous = true;
        }
      }
    }
    out.println(res + ")\n  {");
    buildConstructorCodeJava7(out);
    out.println("  }\n");

    if (previous)  // constructor has arguments
    { out.println("  " + vis + " " + nme + "() { }\n"); } 
  }

  private void buildConstructorCodeJava6(PrintWriter out)
  { for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String ini = att.initialiserJava6();
      if (ini != null)
      { out.println("    " + ini); }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String ini = ast.initialiser();
      if (ini != null)
      { out.println("    " + ini); }
    }

    String code = ""; 
    String nme = getName(); 
    java.util.Map env = new java.util.HashMap(); 
    env.put(nme,"this"); 

    for (int i = 0; i < invariants.size(); i++)
    { Constraint cc = (Constraint) invariants.get(i); 
      code = code + cc.updateFormJava6(env,true); 
    } 
    out.println(code); 
  } 

  private void buildConstructorCodeJava7(PrintWriter out)
  { for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String ini = att.initialiserJava7();
      if (ini != null)
      { out.println("    " + ini); }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String ini = ast.initialiser();
      if (ini != null)
      { out.println("    " + ini); }
    }

    String code = ""; 
    String nme = getName(); 
    java.util.Map env = new java.util.HashMap(); 
    env.put(nme,"this"); 

    for (int i = 0; i < invariants.size(); i++)
    { Constraint cc = (Constraint) invariants.get(i); 
      code = code + cc.updateFormJava7(env,true); 
    } 
    out.println(code); 
  } 

  private void buildConstructorCodeCSharp(PrintWriter out)
  { for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String ini = att.initialiserCSharp();
      if (ini != null)
      { out.println("    " + ini); }
    }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String ini = ast.initialiser();
      if (ini != null)
      { out.println("    " + ini); }
    }

    String code = ""; 
    String nme = getName(); 
    java.util.Map env = new java.util.HashMap(); 
    env.put(nme,"this"); 

    for (int i = 0; i < invariants.size(); i++)
    { Constraint cc = (Constraint) invariants.get(i); 
      code = code + cc.updateFormCSharp(env,true); 
    } 
    out.println(code); 
  } 

  private void buildCSharpConstructor(PrintWriter out)
  { if (isInterface()) { return; } 
    String nme = getName(); 
    String vis = "public"; 
    if (isSingleton()) 
    { vis = "private";
      String inst = "instance_" + nme; 
      out.println("  public static " + nme + " inst()"); 
      out.println("  { if (" + inst + " == null) { " + 
                           inst + " = new " + nme + "(); }"); 
      out.println("    return " + inst + ";"); 
      out.println("  }\n"); 
    } 

    out.print("  " + vis + " " + nme + "(");
    boolean previous = false;
    String res = "";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.constructorParameterCSharp();
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else        
        { res = par;
          previous = true;
        }
      }
    }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String par = ast.constructorParameterCSharp();
      if (par != null)
      { if (previous)
        { res = res + "," + par; }
        else
        { res = par;
          previous = true;
        }
      }
    }
    out.println(res + ")\n  {");
    buildConstructorCodeCSharp(out);
    out.println("  }\n");

    if (previous)  // constructor has arguments
    { out.println("  " + vis + " " + nme + "() { }\n"); } 
  }


  private void buildConstructorCPP(PrintWriter out)
  { // if (isInterface()) { return; } 
    String nme = getName(); 
    if (isSingleton()) 
    { String inst = "instance_" + nme; 
      out.println("  static " + nme + "* inst()"); 
      out.println("  { if (" + inst + " == 0) { " + 
                           inst + " = new " + nme + "(); }"); 
      out.println("    return " + inst + ";"); 
      out.println("  }\n"); 
    } 

    out.print(" " + nme + "(");
    boolean previous = false;
    String res = "";

    Vector directsuperclasses = new Vector(); 
    if (superclass != null) { directsuperclasses.add(superclass); } 
    directsuperclasses.addAll(interfaces); 

    Vector mydatafeatures = allDataFeatures(); 
    Vector inheriteddatafeatures = allInheritedDataFeaturesCPP();  
    Vector alldatafeatures = new Vector(); 
    alldatafeatures.addAll(mydatafeatures); 
    alldatafeatures.addAll(inheriteddatafeatures); 
    Vector actualconspars = new Vector(); 

    for (int i = 0; i < alldatafeatures.size(); i++)
    { ModelElement feat = (ModelElement) alldatafeatures.get(i);
      String par = feat.constructorParameterCPP();
      if (par != null)
      { if (!(mydatafeatures.contains(feat)))
        { actualconspars.add(feat); } 
 
        if (previous)
        { res = res + "," + par; }
        else        
        { res = par;
          previous = true;
        }
      }
    }

    out.print(res + ") "); 

    out.println(superclassesConstructorCalls(directsuperclasses,actualconspars)); 
    out.println("  {");
    buildConstructorCodeCPP(out);
    out.println("  }\n");

    if (previous)  // constructor has arguments
    { out.println("  " + nme + "() { }\n"); } 
    // Set all data features to default values
  }

  private String superclassesConstructorCalls(Vector dsups, Vector inheritedFeats)
  { if (inheritedFeats.size() == 0) { return ""; } 
    String res = " : "; 
    String currentclass = ""; 
    for (int i = 0; i < inheritedFeats.size(); i++) 
    { ModelElement obj = (ModelElement) inheritedFeats.get(i);
      String ename = ""; 
      String pname = ""; 
      Entity ee = firstDefiningClass(dsups, obj); // The direct superclass that has obj as feature
      if (ee == null) { continue; } // should not happen
      ename = ee.getName(); 
      pname = obj.getName() + "x"; 
 
      if (currentclass.equals(""))
      { currentclass = ename; 
        res = res + ename + "(" + pname; 
      } 
      else if (currentclass.equals(ename)) // previous par for this class
      { res = res + "," + pname; } 
      else // new class
      { currentclass = ename; 
        res = res + "), " + ename + "(" + pname; 
      } 
    } 
    return res + ")"; 
  } 


  private void buildConstructorCodeCPP(PrintWriter out)
  { for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String ini = att.initialiserCPP();
      if (ini != null)
      { out.println("    " + ini); }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String ini = ast.initialiserCPP();
      if (ini != null)
      { out.println("    " + ini); }
    }

    String code = ""; 
    String nme = getName(); 
    java.util.Map env = new java.util.HashMap(); 
    env.put(nme,"this"); 

    for (int i = 0; i < invariants.size(); i++)
    { Constraint cc = (Constraint) invariants.get(i); 
      code = code + cc.updateFormCPP(env,true); 
    } 
    out.println(code); 
  } 

  private void buildDestructorCPP(PrintWriter out)
  { // if (isInterface()) { return; } 
    String nme = getName(); 
   
    if (isActualLeaf())
    { out.println("  ~" + nme + "() {"); } 
    else 
    { out.println("  virtual ~" + nme + "() {"); } 

    
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String par = ast.destructorCodeCPP();
      if (par != null)
      { out.println("  " + par); } 
    }
    out.println("  }\n");
  }


  private void buildOperations(Vector entities, Vector types,
                               PrintWriter out)
  { String interfaceinnerclass = ""; 
    if (isInterface())
    { interfaceinnerclass = "\n" + 
                            "  class " + getName() + "Ops\n" + 
                            "  {\n"; 
    } 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isFrozen()) { } 
      else 
      { String par = att.setOperation(this,invariants,entities,types);
        if (par != null)
        { out.println("  " + par + "\n"); }
        if (att.isMultiple())
        { par = att.addremOperation(this); 
          if (par != null) 
          { out.println("  " + par + "\n"); }
        } 


        if (isInterface())
        { par = "";  // att.setAllInterfaceOperation(getName()); }
          interfaceinnerclass = interfaceinnerclass + "  " + 
                                att.setAllOperation(getName()) + "\n\n"; 
        } 
        else 
        { par = att.setAllOperation(getName()); } 

        if (par != null)
        { out.println("  " + par + "\n"); }
      } 
    }

    Vector allinvfeats = Constraint.allFeaturesUsedIn(invariants); 
    Vector oldatts = allInheritedAttributes(); 
    for (int j = 0; j < oldatts.size(); j++) 
    { Attribute oldatt = (Attribute) oldatts.get(j); 
      if (allinvfeats.contains(oldatt.getName()))
      { String redefinedop = oldatt.setOperation(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } // likewise for associations

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      // no ops for frozen ones, not remove if addOnly
      if (ast.isFrozen())
      { continue; }

      String par = "";  
      if (isInterface())
      { String ipar = ast.setInterfaceOperation(this); 
        out.println(ipar);  
      } 
      else 
      { par = ast.setOperation(this,invariants,entities,types); 
        if (par != null)
        { out.println("  " + par + "\n"); }  // includes add, rem
      } 

      par = ast.setAllOperation(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }   
      if (ast.getCard2() != ONE)
      { String pp = ast.addAllOperation(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        } 
        pp = ast.removeAllOperation(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.unionAllOperation(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.subtractAllOperation(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
      }
    }

    Vector oldasts = allInheritedAssociations(); 
    for (int j = 0; j < oldasts.size(); j++) 
    { Association oldast = (Association) oldasts.get(j); 
      if (allinvfeats.contains(oldast.getRole2()))
      { String redefinedop = oldast.setOperation(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } 



    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.getOperation(this);
      if (par != null)
      { out.println("  " + par + "\n"); }
      par = att.getAllOperation(this,getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
      par = att.getAllOrderedOperation(this,getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String par = ""; 
      if (isInterface())
      { String ipar = ast.getInterfaceOperation(); 
        out.println(ipar);  
      } 
      else 
      { par = ast.getOperation();
        if (par != null)
        { out.println("  " + par + "\n"); }
      } 
      par = ast.getAllOperation(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
      par = ast.getAllOrderedOperation(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
    }

    if (isInterface())
    { out.println(interfaceinnerclass); 
      out.println("  }\n"); 
    } 

    if (isActive())
    { out.println("  private synchronized void run_step() { "); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { Statement ss = bf.getSm().methodStepCode(); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        ss.typeCheck(types,entities,contexts,new Vector()); 
        ss.displayJava(null,out);
      }
      out.println(" }\n");
    } 
    // code given by statechart

    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      // System.out.println(op + " " + op.isQuery()); 
      String optext = op.getOperationCode(this,entities,types); 
      if (optext != null) 
      { out.println("  " + optext + "\n"); 
        op.setText(optext); 
      }
    } 
  }
  // also generate an "equals" method based on "toString", which 
  // prints out all attribute values: (name) val1,val2,...,valn 

  private void buildOperationsJava6(Vector entities, Vector types,
                               PrintWriter out)
  { String interfaceinnerclass = ""; 
    if (isInterface())
    { interfaceinnerclass = "\n" + 
                            "  class " + getName() + "Ops\n" + 
                            "  {\n"; 
    } 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isFrozen()) { } 
      else 
      { String par = att.setOperationJava6(this,invariants,entities,types);
        if (par != null)
        { out.println("  " + par + "\n"); }

        if (isInterface())
        { par = "";  // att.setAllInterfaceOperation(getName()); }
          interfaceinnerclass = interfaceinnerclass + "  " + 
                                att.setAllOperationJava6(getName()) + "\n\n"; 
        } 
        else 
        { par = att.setAllOperationJava6(getName()); } 

        if (par != null)
        { out.println("  " + par + "\n"); }
      } 
    }

    Vector allinvfeats = Constraint.allFeaturesUsedIn(invariants); 
    Vector oldatts = allInheritedAttributes(); 
    for (int j = 0; j < oldatts.size(); j++) 
    { Attribute oldatt = (Attribute) oldatts.get(j); 
      if (allinvfeats.contains(oldatt.getName()))
      { String redefinedop = oldatt.setOperationJava6(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } // likewise for associations

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      // no ops for frozen ones, not remove if addOnly
      if (ast.isFrozen())
      { continue; } 

      String par = null; 
      if (isInterface())
      { par = ast.setInterfaceOperationJava6(this); 
        out.println(par); 
        // continue; 
      } 
      else 
      { par = ast.setOperationJava6(this,invariants,entities,types); }

      if (par != null)
      { out.println("  " + par + "\n"); }  // includes add, rem

      par = ast.setAllOperationJava6(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }   

      if (ast.getCard2() != ONE)
      { String pp = ast.addAllOperationJava6(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.removeAllOperationJava6(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.unionAllOperationJava6(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.subtractAllOperationJava6(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
      }
    }

    Vector oldasts = allInheritedAssociations(); 
    for (int j = 0; j < oldasts.size(); j++) 
    { Association oldast = (Association) oldasts.get(j); 
      if (allinvfeats.contains(oldast.getRole2()))
      { String redefinedop = oldast.setOperationJava6(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } 



    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.getOperationJava6(this);
      if (par != null)
      { out.println("  " + par + "\n"); }

      par = att.getAllOperationJava6(this,getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }

      par = att.getAllOrderedOperationJava6(this,getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      if (isInterface())
      { String ipar = ast.getInterfaceOperationJava6(); 
        out.println(ipar); 
        continue; 
      } 

      String par = ast.getOperationJava6();
      if (par != null)
      { out.println("  " + par + "\n"); }

      par = ast.getAllOperationJava6(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }

      par = ast.getAllOrderedOperationJava6(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
    }

    if (isInterface())
    { out.println(interfaceinnerclass); 
      out.println("  }\n"); 
    } 

    if (isActive())
    { out.println("  private synchronized void run_step() { "); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { Statement ss = bf.getSm().methodStepCode(); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        ss.typeCheck(types,entities,contexts,new Vector()); 
        ss.displayJava(null,out);
      }
      out.println(" }\n");
    } 
    // code given by statechart

    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      System.out.println(op + " " + op.isQuery()); 
      String optext = op.getOperationCodeJava6(this,entities,types); 
      if (optext != null) 
      { out.println("  " + optext + "\n"); }
    } 
  }
  // also generate an "equals" method based on "toString", which 
  // prints out all attribute values: (name) val1,val2,...,valn 

  private void buildOperationsJava7(Vector entities, Vector types,
                               PrintWriter out)
  { String interfaceinnerclass = ""; 
    if (isInterface())
    { interfaceinnerclass = "\n" + 
                            "  class " + getName() + "Ops\n" + 
                            "  {\n"; 
    } 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isFrozen()) { } 
      else 
      { String par = att.setOperationJava7(this,invariants,entities,types);   
        if (par != null)
        { out.println("  " + par + "\n"); }

        if (isInterface())
        { par = "";  // att.setAllInterfaceOperation(getName()); }
          interfaceinnerclass = interfaceinnerclass + "  " + 
                                att.setAllOperationJava7(getName()) + "\n\n"; 
        } 
        else 
        { par = att.setAllOperationJava7(getName()); } 

        if (par != null)
        { out.println("  " + par + "\n"); }
      } 
    }

    Vector allinvfeats = Constraint.allFeaturesUsedIn(invariants); 
    Vector oldatts = allInheritedAttributes(); 
    for (int j = 0; j < oldatts.size(); j++) 
    { Attribute oldatt = (Attribute) oldatts.get(j); 
      if (allinvfeats.contains(oldatt.getName()))
      { String redefinedop = oldatt.setOperationJava7(this,invariants,entities,types);  
        out.println("  " + redefinedop + "\n"); 
      } 
    } // likewise for associations

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      // no ops for frozen ones, not remove if addOnly
      if (ast.isFrozen())
      { continue; } 

      String par = null; 
      if (isInterface())
      { par = ast.setInterfaceOperationJava7(this); 
        out.println(par); 
        // continue; 
      } 
      else 
      { par = ast.setOperationJava7(this,invariants,entities,types); }

      if (par != null)
      { out.println("  " + par + "\n"); }  // includes add, rem

      par = ast.setAllOperationJava7(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }   

      if (ast.getCard2() != ONE)
      { String pp = ast.addAllOperationJava7(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.removeAllOperationJava7(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.unionAllOperationJava7(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.subtractAllOperationJava7(getName());
        if (pp != null)
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + "  " + pp + "\n"; } 
          else 
          { out.println("  " + pp + "\n"); }
        }
      }
    }

    Vector oldasts = allInheritedAssociations(); 
    for (int j = 0; j < oldasts.size(); j++) 
    { Association oldast = (Association) oldasts.get(j); 
      if (allinvfeats.contains(oldast.getRole2()))
      { String redefinedop = oldast.setOperationJava7(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } 



    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.getOperationJava7(this);  
      if (par != null)
      { out.println("  " + par + "\n"); }

      par = att.getAllOperationJava7(this,getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }

      par = att.getAllOrderedOperationJava7(this,getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      if (isInterface())
      { String ipar = ast.getInterfaceOperationJava7(); 
        out.println(ipar); 
        continue; 
      } 

      String par = ast.getOperationJava7();
      if (par != null)
      { out.println("  " + par + "\n"); }

      par = ast.getAllOperationJava7(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }

      par = ast.getAllOrderedOperationJava7(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + "  " + par + "\n"; } 
        else 
        { out.println("  " + par + "\n"); }
      }
    }

    if (isInterface())
    { out.println(interfaceinnerclass); 
      out.println("  }\n"); 
    } 

    if (isActive())
    { out.println("  private synchronized void run_step() { "); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { Statement ss = bf.getSm().methodStepCode(); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        ss.typeCheck(types,entities,contexts,new Vector()); 
        ss.displayJava(null,out);   // Java7?
      }
      out.println(" }\n");
    } 
    // code given by statechart

    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      System.out.println(op + " " + op.isQuery()); 
      String optext = op.getOperationCodeJava7(this,entities,types); 
      if (optext != null) 
      { out.println("  " + optext + "\n"); }
    } 
  }

  private void buildCSharpOperations(Vector entities, Vector types,
                               PrintWriter out)
  { String interfaceinnerclass = ""; 
    if (isInterface())
    { interfaceinnerclass = "\n" + 
                            "  class " + getName() + "Ops\n" + 
                            "  {\n"; 
    } // This class will go after the class for the interface itself. 


    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isFrozen()) { } 
      else 
      { String par = att.setOperationCSharp(this,invariants,entities,types);
        if (par != null)
        { out.println("  " + par + "\n"); }

        par = att.setAllOperationCSharp(getName());
        if (par != null) 
        { if (isInterface())
          { interfaceinnerclass = interfaceinnerclass + par + "\n  "; } 
          else 
          { out.println("  " + par + "\n"); }
        }
      } 
    }

    Vector allinvfeats = Constraint.allFeaturesUsedIn(invariants); 
    Vector oldatts = allInheritedAttributes(); 
    for (int j = 0; j < oldatts.size(); j++) 
    { Attribute oldatt = (Attribute) oldatts.get(j); 
      if (allinvfeats.contains(oldatt.getName()))
      { String redefinedop = oldatt.setOperationCSharp(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } // likewise for associations

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      // no ops for frozen ones, not remove if addOnly
      if (ast.isFrozen())
      { continue; } 
      if (isInterface())
      { String ipar = ast.setInterfaceOperationCSharp(this); 
        out.println(ipar); 
        // continue; 
      } 
      else 
      { String apar = ast.setOperationCSharp(this,invariants,entities,types); 
        if (apar != null)
        { out.println("  " + apar + "\n"); }  // includes add, rem
      } 

      String par = ast.setAllOperationCSharp(getName()); 
      if (par != null)
      { if (isInterface())
        { interfaceinnerclass = interfaceinnerclass + par + "\n"; } 
        else
        { out.println("  " + par + "\n"); }
      }   
      if (ast.getCard2() != ONE)
      { String pp = ast.addAllOperationCSharp(getName());
        if (pp != null)
        { if (isInterface()) 
          { interfaceinnerclass = interfaceinnerclass + pp + "\n"; } 
          else
          { out.println("  " + pp + "\n"); }
        } 
        pp = ast.removeAllOperationCSharp(getName());
        if (pp != null)
        { if (isInterface()) 
          { interfaceinnerclass = interfaceinnerclass + pp + "\n"; } 
          else
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.unionAllOperationCSharp(getName());
        if (pp != null)
        { if (isInterface()) 
          { interfaceinnerclass = interfaceinnerclass + pp + "\n"; } 
          else
          { out.println("  " + pp + "\n"); }
        }
        pp = ast.subtractAllOperationCSharp(getName());
        if (pp != null)
        { if (isInterface()) 
          { interfaceinnerclass = interfaceinnerclass + pp + "\n"; } 
          else
          { out.println("  " + pp + "\n"); }
        }
      }
    }

    Vector oldasts = allInheritedAssociations(); 
    for (int j = 0; j < oldasts.size(); j++) 
    { Association oldast = (Association) oldasts.get(j); 
      if (allinvfeats.contains(oldast.getRole2()))
      { String redefinedop = oldast.setOperationCSharp(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.getOperationCSharp(this);
      if (par != null)
      { out.println("  " + par + "\n"); }
      par = att.getAllOperationCSharp(this,getName()); 
      if (par != null)
      { if (isInterface()) 
        { interfaceinnerclass = interfaceinnerclass + par + "\n"; } 
        else
        { out.println("  " + par + "\n"); }
      }
      par = att.getAllOrderedOperationCSharp(this,getName()); 
      if (par != null)
      { if (isInterface()) 
        { interfaceinnerclass = interfaceinnerclass + par + "\n"; } 
        else
        { out.println("  " + par + "\n"); }
      }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      if (isInterface())
      { String ipar = ast.getInterfaceOperationCSharp(); 
        out.println(ipar); 
        // continue; 
      } 
      else 
      { String apar = ast.getOperationCSharp();
        if (apar != null)
        { out.println("  " + apar + "\n"); }
      } 

      String par = ast.getAllOperationCSharp(getName()); 
      if (par != null)
      { if (isInterface()) 
        { interfaceinnerclass = interfaceinnerclass + par + "\n"; } 
        else
        { out.println("  " + par + "\n"); }
      }
      par = ast.getAllOrderedOperationCSharp(getName()); 
      if (par != null)
      { if (isInterface()) 
        { interfaceinnerclass = interfaceinnerclass + par + "\n"; } 
        else
        { out.println("  " + par + "\n"); }
      }
    }

    if (isActive())
    { out.println("  private synchronized void run_step() { "); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { Statement ss = bf.getSm().methodStepCode(); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        ss.typeCheck(types,entities,contexts,new Vector()); 
        ss.displayJava(null,out);
      }
      out.println(" }\n");
    } 
    // code given by statechart

    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      System.out.println(op + " " + op.isQuery()); 
      String optext = op.getOperationCodeCSharp(this,entities,types); 
      if (optext != null) 
      { out.println("  " + optext + "\n"); }
    } 

    if (isInterface())
    { out.println("}\n\n");  // end of class getName()
      out.println(interfaceinnerclass); 
    } 
  }
  // also generate an "equals" method based on "toString", which 
  // prints out all attribute values: (name) val1,val2,...,valn 

  private void buildOperationsCPP(Vector entities, Vector types,
                                  PrintWriter out, PrintWriter out2)
  { Vector declarations = new Vector(); 

    out2.println(privateAux);     

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isFrozen()) { } 
      else 
      { String par = att.setOperationCPP(this,invariants,entities,types);
        if (par != null)
        { out.println("  " + par + "\n"); }

        // if (isInterface())
        // { par = ""; } // att.setAllInterfaceOperation(getName()); } 
        // else 
        par = att.setAllOperationCPP(getName(),declarations);  

        if (par != null)
        { out2.println("  " + par + "\n"); }
      } 
    }

    for (int i = 0; i < declarations.size(); i++) 
    { String decl = (String) declarations.get(i); 
      out.println(decl); 
    } 
    declarations.clear(); 

    Vector allinvfeats = Constraint.allFeaturesUsedIn(invariants); 
    Vector oldatts = allInheritedAttributes(); 
    for (int j = 0; j < oldatts.size(); j++) 
    { Attribute oldatt = (Attribute) oldatts.get(j); 
      if (allinvfeats.contains(oldatt.getName()))
      { String redefinedop = oldatt.setOperationCPP(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } // likewise for associations

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      // no ops for frozen ones, not remove if addOnly
      if (ast.isFrozen())
      { continue; } 
      // if (isInterface())
      // { String ipar = ast.setInterfaceOperation(this); 
      //   out.println(ipar); 
      //   continue; 
      // } 
      String par = ast.setOperationCPP(this,invariants,entities,types); 
      if (par != null)
      { out.println("  " + par + "\n"); }  // includes add, rem
      par = ast.setAllOperationCPP(getName(), declarations); 
      if (par != null)
      { out2.println("  " + par + "\n"); }  
      if (ast.getCard2() != ONE)
      { String pp = ast.addAllOperationCPP(getName(), declarations);
        if (pp != null)
        { out2.println("  " + pp + "\n"); }
        pp = ast.removeAllOperationCPP(getName(), declarations);
        if (pp != null)
        { out2.println("  " + pp + "\n"); }
        pp = ast.unionAllOperationCPP(getName(), declarations);
        if (pp != null)
        { out2.println("  " + pp + "\n"); }
        pp = ast.subtractAllOperationCPP(getName(), declarations);
        if (pp != null)
        { out2.println("  " + pp + "\n"); }
      }
    }

    Vector oldasts = allInheritedAssociations(); 
    for (int j = 0; j < oldasts.size(); j++) 
    { Association oldast = (Association) oldasts.get(j); 
      if (allinvfeats.contains(oldast.getRole2()))
      { String redefinedop = oldast.setOperationCPP(this,invariants,entities,types); 
        out.println("  " + redefinedop + "\n"); 
      } 
    } 

    for (int i = 0; i < declarations.size(); i++) 
    { String decl = (String) declarations.get(i); 
      out.println(decl); 
    } 


    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String par = att.getOperationCPP(this);
      if (par != null)
      { out.println("  " + par + "\n"); }
      par = att.getAllOperationCPP(this,getName()); 
      if (par != null)
      { out.println("  " + par + "\n"); }
      par = att.getAllOrderedOperationCPP(this,getName()); 
      if (par != null)
      { out.println("  " + par + "\n"); }
    }

    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      // if (isInterface())
      // { String ipar = ast.getInterfaceOperation(); 
      //   out.println(ipar); 
      //   continue; 
      // } 

      String par = ast.getOperationCPP();
      if (par != null)
      { out.println("  " + par + "\n"); }
      par = ast.getAllOperationCPP(getName()); 
      if (par != null)
      { out.println("  " + par + "\n"); }
      par = ast.getAllOrderedOperationCPP(getName()); 
      if (par != null)
      { out.println("  " + par + "\n"); }
    }

    /* if (isActive())
    { out.println("  private synchronized void run_step() { "); 
      BehaviouralFeature bf = getOperation("run"); 
      if (bf != null && bf.getSm() != null)
      { Statement ss = bf.getSm().methodStepCode(); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        ss.typeCheck(types,entities,contexts,new Vector()); 
        ss.displayJava(null,out);
      }
      out.println(" }\n");
    } */ 
    // code given by statechart

    Vector eopdecs = new Vector(); 

    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      // System.out.println(op + " query: " + op.isQuery()); 
      String optext = op.getOperationCodeCPP(this,entities,types,eopdecs); 
      if (optext != null) 
      { out2.println("  " + optext + "\n"); }
    } 

    for (int g = 0; g < eopdecs.size(); g++) 
    { String eopdec = (String) eopdecs.get(g); 
      out.println(eopdec); 
    } 
  }
  // also generate an "equals" method based on "toString", which 
  // prints out all attribute values: (name) val1,val2,...,valn 


  // Controller operations:
  public Vector sensorOperationsCode(Vector cons,Vector entities,
                                     Vector types) 
  { Vector res = new Vector();
    // if (isInterface()) { return res; }   
    // The operations should be provided instead for subclasses

    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec);   // There should not also be a primary key in this
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      if (att.isFrozen()) { }  // why externalise this?
      else 
      { res.addAll(att.senOperationsCode(cons,this,entities,types)); } 
      if (att.isMultiple())
      { res.addAll(att.addremOperationsCode(this)); } 
    }
    return res;
  }  // But only setatt for sensor atts goes in the external interface

  public Vector sensorOperationsCodeJava7(Vector cons,Vector entities,
                                     Vector types) 
  { Vector res = new Vector();
    // if (isInterface()) { return res; }   
    // The operations should be provided instead for subclasses

    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec);   // There should not also be a primary key in this
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      if (att.isFrozen()) { }  // why externalise this?
      else 
      { res.addAll(att.senOperationsCodeJava7(cons,this,entities,types)); } 
    }
    return res;
  }  // But only setatt for sensor atts goes in the external interface

  public Vector sensorOperationsCodeCSharp(Vector cons,Vector entities,
                                     Vector types) 
  // for Controller
  { Vector res = new Vector();
    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec);   // There should not also be a primary key in this
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      if (att.isFrozen()) { }  // why externalise this?
      else 
      { res.addAll(att.senOperationsCodeCSharp(cons,this,entities,types)); } 
    }
    return res;
  }  // But only setatt for sensor atts goes in the external interface


  public Vector sensorOperationsCodeCPP(Vector cons,Vector entities,
                                        Vector types) 
  { Vector res = new Vector();
    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec);   // There should not also be a primary key in this
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      if (att.isFrozen()) { }  // why externalise this?
      else 
      { res.addAll(att.senOperationsCodeCPP(cons,this,entities,types)); } 
    }
    return res;
  }  // But only setatt for sensor atts goes in the external interface

  public Vector associationOperationsCode(Vector cons,Vector entities,
                                          Vector types)
  { Vector res = new Vector(); 
    // if (isInterface()) { return res; } // Provided by subclasses instead

    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      res.addAll(ast.setOperationsCode(cons,this,entities,types)); 
      if (ast.getCard2() != ONE && !ast.isFrozen())
      { res.addAll(ast.addremOperationsCode(cons,this,entities,types)); } 
    }
    return res;
  }  // not for frozen, addOnly associations

  public Vector associationOperationsCodeJava6(Vector cons,Vector entities,
                                          Vector types)
  { Vector res = new Vector(); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      res.addAll(ast.setOperationsCodeJava6(cons,this,entities,types)); 
      if (ast.getCard2() != ONE && !ast.isFrozen())
      { res.addAll(ast.addremOperationsCodeJava6(cons,this,entities,types)); } 
    }
    return res;
  }  // not for frozen, addOnly associations

  public Vector associationOperationsCodeJava7(Vector cons,Vector entities,
                                          Vector types)
  { Vector res = new Vector(); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      res.addAll(ast.setOperationsCodeJava7(cons,this,entities,types)); 
      if (ast.getCard2() != ONE && !ast.isFrozen())
      { res.addAll(ast.addremOperationsCodeJava7(cons,this,entities,types)); } 
    }
    return res;
  }  // not for frozen, addOnly associations

  public Vector associationOperationsCodeCSharp(Vector cons,Vector entities,
                                          Vector types)
  { Vector res = new Vector(); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      res.addAll(ast.setOperationsCodeCSharp(cons,this,entities,types)); 
      if (ast.getCard2() != ONE && !ast.isFrozen())
      { res.addAll(ast.addremOperationsCodeCSharp(cons,this,entities,types)); } 
    }
    return res;
  }  // not for frozen, addOnly associations

  public Vector associationOperationsCodeCPP(Vector cons,Vector entities,
                                          Vector types)
  { Vector res = new Vector(); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      res.addAll(ast.setOperationsCodeCPP(cons,this,entities,types)); 
      if (ast.getCard2() != ONE && !ast.isFrozen())
      { res.addAll(ast.addremOperationsCodeCPP(cons,this,entities,types)); } 
    }
    return res;
  }  // not for frozen, addOnly associations


  public void buildMainOperation(Vector entities, Vector types, PrintWriter out)
  { String nme = getName(); 
    out.println("  public static void main(String[] args) \n"); 
    out.println("  { \n"); 
      // + nme + " " + nme.toLowerCase() + 
      //    "x = Controller.inst().create" + nme + "();\n");
    Vector contexts = new Vector(); 
    contexts.add(this); 

    java.util.Map env = new java.util.HashMap(); 
    
    if (activity == null) 
    { out.println("  }\n\n"); } 
    else     
    { activity.typeCheck(types,entities,contexts,new Vector());  // or empty contxt 
      String code = activity.updateForm(env,false,types,entities,new Vector());
      out.println(code + "\n }\n\n");
    } 
  } 

  public String genMainOperation(Vector entities, Vector types)
  { if (activity == null) { return null; } 
    String nme = getName(); 
    String res = "  public static void main(String[] args) \n"; 
    res = res + "  { \n"; 
    Vector contexts = new Vector(); 
    contexts.add(this); 
    java.util.Map env = new java.util.HashMap(); 

    activity.typeCheck(types,entities,contexts,new Vector()); 
    String code = activity.updateForm(env,false,types,entities,new Vector());
    res = res + code + "\n }\n\n";
    return res; 
  } 

  public boolean isSensorAttribute(String data)
  { for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.getName().equals(data) && att.isSensor())
      { return true; } 
    } 
    return false; 
  } 

  public boolean isInputAttribute(String data)
  { for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.getName().equals(data) && att.isInput())
      { return true; } 
    } 
    return false; 
  } 

  public boolean hasCycle()
  { Vector path = new Vector();
    for (int i = 0; i < associations.size(); i++)
    { Association ass = (Association) associations.get(i);
      Entity ent = ass.getEntity2();
      if (ent == this) { }  // ignore self-relations
      else 
      { path.add(ent); }
    }

    for (int j = 0; j < path.size(); j++)
    { Entity e = (Entity) path.get(j);
      boolean res = e.findCycle(this,path);
      if (res) { return true; }
    }
    return false;
  }

  private boolean findCycle(Entity e, Vector path)
  { for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      Entity ent = ast.getEntity2();
      if (ent == e) { return true; }
      if (path.contains(ent)) { }  // already visited ent
      else 
      { path.add(ent);
        boolean res = ent.findCycle(e,path);
        if (res) { return true; }
      }
    }
    return false;
  }

  public String generateEquals()
  { String nme = getName();
    String ex = nme.toLowerCase() + "x"; 
    String test = "";
    boolean previous = false;
    for (int i = 0; i < attributes.size(); i++)
    { String cond = "";
      Attribute att = (Attribute) attributes.get(i);
      Type t = att.getType();
      String attnme = att.getName();
      if (t.getName().equals("String"))
      { cond = ex + ".get" + attnme + "().equals(" +
               attnme + ")";
      }
      else
      { cond = ex + ".get" + attnme + "() == " + attnme; }
      if (previous)
      { test = test + " && " + cond; }
      else
      { test = cond;
        previous = true;
      }
    } // and roles.

    String res = "  public boolean equals(Object " + ex + ")\n";
    res = res + "  { if (" + ex + " == null) { return false; }\n";
    res = res + "    if (" + ex + " instanceof " + nme + ")\n";
    res = res + "    { return " + test + "; }\n"; 
    res = res + "    return false; }";
    return res;
  }

  public String generateToStringOp()
  { if (isInterface())
    { return "  public String toString();\n"; } 

    String nme = "(" + getName() + ")";
    String res = "  public String toString()\n" +
      "  { String _res_ = \"" + nme + " \";\n";
    
    // Vector datts = new Vector(); 
    // datts.addAll(attributes); 
    // datts.addAll(allInheritedAttributes()); 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String getop = att.getName();
      if (i < attributes.size() - 1)
      { getop = getop + " + \",\""; }
      res = res + "    _res_ = _res_ + " + getop + ";\n";
    }
    if (superclass != null) 
    { res = res + "    return _res_ + super.toString();\n  }"; } 
    else 
    { res = res + "    return _res_;\n  }"; }
    return res;
  } // and roles

  public String generateCSharpToStringOp()
  { if (isInterface())
    { return "  public override string ToString();\n"; } 

    String nme = "(" + getName() + ")";
    String res = "  public override string ToString()\n" +
      "  { string _res_ = \"" + nme + " \";\n";
    
    // Vector datts = new Vector(); 
    // datts.addAll(attributes); 
    // datts.addAll(allInheritedAttributes()); 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String getop = att.getName();
      if (i < attributes.size() - 1)
      { getop = getop + " + \",\""; }
      res = res + "    _res_ = _res_ + " + getop + ";\n";
    }
    if (superclass != null) 
    { res = res + "    return _res_ + base.ToString();\n  }"; } 
    else 
    { res = res + "    return _res_;\n  }"; }
    return res;
  } // and roles

  public String generateToStringOpCPP()
  { // if (isInterface())
    // { return "  public override string ToString();\n"; } 

    String ename = getName(); 
    String nme = "(" + ename + ")";
    String res = "  friend ostream& operator<<(ostream& s, " + ename + "& x)\n" +
      "  { return s << \"" + nme + " \" ";
    
    // Vector datts = new Vector(); 
    // datts.addAll(attributes); 
    // datts.addAll(allInheritedAttributes()); 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String getop = "x.get" + att.getName() + "()";
      if (i < attributes.size() - 1)
      { getop = getop + " << \",\""; }
      res = res + " << " + getop;
    }
    res = res + " << endl; }\n\n"; 
    // if (superclass != null) 
    // { res = res + "    return _res_ + base.ToString();\n  }"; } 
    // else 
    // { res = res + "    return _res_;\n  }"; }
    return res;
  } // and roles


  public void asTextModel(PrintWriter out) 
  { String nme = getName(); 
    out.println(nme + " : Entity"); 
    out.println(nme + ".name = \"" + nme + "\"");
    String tid = Identifier.nextIdentifier("");  
    out.println(nme + ".typeId = \"" + tid + "\""); 
    if (isAbstract())
    { out.println(nme + ".isAbstract = true"); }
    if (isInterface())
    { out.println(nme + ".isInterface = true"); }

    for (int i = 0; i < stereotypes.size(); i++)
    { out.println("\"" + stereotypes.get(i) + "\" : " + nme + ".stereotypes"); }

    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attid = att.saveModelData(out); 
    }     
  } 

  public void asTextModel2(PrintWriter out, Vector entities, Vector types) 
  { String nme = getName(); 

    Vector allops = allOperations(); 
    // Vector subs = getAllSubclasses();
    Vector subs = getActualLeafSubclasses();   
    Vector opnames = ModelElement.getNames(operations); 

    for (int i = 0; i < operations.size(); i++)
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i);
      String opname = op.getName(); 
      String opid = op.saveModelData(out, this, entities, types);
      out.println(opid + " : " + nme + ".ownedOperation");
      for (int j = 0; j < subs.size(); j++) 
      { Entity esub = (Entity) subs.get(j); 
        // if (esub.hasConcreteOperation(opname)) 
        { out.println(esub.getName() + " : " + opid + ".definers"); } 
      } 
    }

    if (superclass != null) 
    { out.println(superclass.getName() + " : " + nme + ".superclass"); 
      /* Vector inheritedops = superclass.allInheritedOperations(); 
      for (int k = 0; k < inheritedops.size(); k++) 
      { BehaviouralFeature bf = (BehaviouralFeature) inheritedops.get(k); 
        if (opnames.contains(bf.getName()))  
        { } 
      }     */ 
    } 

    for (int j = 0; j < subclasses.size(); j++) 
    { Entity sub = (Entity) subclasses.get(j); 
      out.println(sub.getName() + " : " + nme + ".subclasses"); 
    } 


  } // is Abstract also

  public void saveEMF(PrintWriter out)
  { String nme = "class " + getName();
    if (isAbstract())
    { nme = "abstract " + nme; } 
    if (superclass != null) 
    { nme = nme + " extends " + superclass; }  
    out.println(nme + " {");
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      att.saveEMF(out); 
    }  
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      ast.saveEMF(out); 
    }
    out.println("}\n\n");   
  } 

  public String getKM3()
  { String nme = "class " + getName();
    if (isAbstract())
    { nme = "abstract " + nme; } 
    if (superclass != null) 
    { nme = nme + " extends " + superclass; }
  
    String res = "  " + nme + " {\n\r";

    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      res = res + att.getKM3() + "\n\r"; 
    }

    res = res + "\n\r";
   
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      res = res + ast.getKM3() + "\n\r"; 
    }
   
    res = res + "\n\r";
   
    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      res = res + op.getKM3(); 
    } 

    res = res + "  }\n\r\n\r";
    return res;    
  } // and operations

  public void saveKM3(PrintWriter out)
  { String nme = "class " + getName();
    if (isAbstract())
    { nme = "abstract " + nme; } 
    if (superclass != null) 
    { nme = nme + " extends " + superclass; }  
    out.println("  " + nme + " {");
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      att.saveKM3(out); 
    }
    out.println();   
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      ast.saveKM3(out); 
    }
    out.println(); 
    for (int i = 0; i < operations.size(); i++) 
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i); 
      op.saveKM3(out); 
    }
    out.println("  }\n\n");   
  } // and operations

  public void saveSimpleKM3(PrintWriter out)
  { String res = ""; 
    if (superclass != null) 
    { res = getName() + " extends " + superclass; }  
    out.println(res); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      ast.saveSimpleKM3(out); 
    }
    out.println(); 
  } // and operations

  public void saveEcore(PrintWriter out)
  { String res = "<eClassifiers xsi:type=\"ecore:EClass\" "; 
    res = res + "name=\"" + getName() + "\"";
    // if (isAbstract())
    // { nme = "abstract " + nme; } 
    if (superclass != null) 
    { res = res + " eSuperTypes=\"#//" + superclass + "\""; }  
    out.println(res + ">");
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      att.saveEcore(out); 
    }  
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      ast.saveEcore(out); 
    }
    out.println("</eClassifiers>");   
  } 

  public String saveData()  // also save initval and cardinality
  { // saveData(getName() + ".srs"); // and stereotypes
    // return getName() + ".srs"; 
    if (isDerived()) 
    { return ""; } 

    String res = superclass + " " + " " + cardinality + " ";
    for (int p = 0; p < stereotypes.size(); p++) 
    { res = res + stereotypes.get(p) + " "; } 
    res = res + "\n"; 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String aname = att.getName();
      Type t = att.getType();
      int m = att.getKind();
      boolean froz = att.isFrozen(); 
      boolean uniq = att.isUnique(); 
      boolean scope = att.isClassScope(); 
      res = res + aname + " " + t + " " + m + " " + froz + " " + uniq + 
            " " + scope;
      if (i < attributes.size() - 1)
      { res = res + " "; }
    }
    return res;
  }   // for each op, save name, type, parameters, pre, post

  public void saveAllOps(PrintWriter out)
  { for (int i = 0; i < operations.size(); i++)
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i);
      op.saveData(out);
    }
  }

  public String saveAsUSEData() 
  { String res =  "class " + getName() + "\n";
    
    if (attributes.size() > 0) { res = res + "attributes\n"; } 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String aname = att.getName();
      Type t = att.getType();
      res = res + aname + " : " + t.getUMLName() + "\n";
    }

    if (operations.size() > 0) { res = res + "operations\n"; } 
    for (int i = 0; i < operations.size(); i++)
    { BehaviouralFeature op = (BehaviouralFeature) operations.get(i);
      res = res + op.saveAsUSEData();
    }
    return res + "end\n\n";
  }   // for each op, save name, type, parameters, pre, post

  public String saveAsZ3Data() 
  { String ename = getName(); 
    String res =  "(declare-sort " + ename + ")\n";
    
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String aname = att.getName();
      Type t = att.getType();
      res = res + "(declare-fun " + aname + " (" + ename + ") " + t.getZ3Name() + ")\n";
    }

    for (int j = 0; j < associations.size(); j++) 
    { Association ast = (Association) associations.get(j); 
      res = res + ast.saveAsZ3Data(); 
    } 

    for (int k = 0; k < invariants.size(); k++) 
    { Constraint con = (Constraint) invariants.get(k); 
      Type otype = new Type(this);  
      BasicExpression sourceVar = new BasicExpression(getName().toLowerCase()); 
      sourceVar.setType(otype); 
      Expression ante = con.antecedent(); 
      Expression succ = con.succedent(); 
      Expression rante = ante.addReference(sourceVar,otype); 
      Expression rsucc = succ.addReference(sourceVar,otype); 
      
      Expression cexp; 
      if ("true".equals(rante + ""))
      { cexp = rsucc; } 
      else 
      { cexp = new BinaryExpression("=>",rante,rsucc); } 
 
      res = res + (new BinaryExpression("!",
                         new BinaryExpression(":",sourceVar,
                               new BasicExpression(otype + "")),cexp)).toZ3() + "\n";
    }  

    return res + "\n\n";
  }   // for each op, save name, type, parameters, pre, post

  public void saveData(String fileName)
  { int n = attributes.size(); 
    int m = associations.size(); 
    String[] attnames = new String[n]; 
    String[] atttypes = new String[n]; // do matching in reconstruction
    int[] attmodality = new int[n]; 

    for (int i = 0; i < n; i++)
    { Attribute att = (Attribute) attributes.get(i); 
      attnames[i] = att.getName(); 
      atttypes[i] = att.getType().getName(); 
      attmodality[i] = att.getKind(); 
    } 

    try
    { ObjectOutputStream out =
        new ObjectOutputStream(
          new FileOutputStream(fileName));
      out.writeObject(getName()); 
      out.writeObject(attnames); 
      out.writeObject(atttypes); 
      out.writeObject(attmodality); 
      out.close();
      System.out.println("Written data");
    }
    catch (IOException e)
    { System.err.println("Error in writing " + getName()); }
  }

  public String getLookupParameters()
  { String res = "";
    boolean previous = false;
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attn = att.getName();
      Type t = att.getType();
      String tname = t.getName();
      String par = tname + " " + attn + "x";
      if (previous)
      { res = res + ", " + par; }
      else
      { res = par;
        previous = true;
      }
    }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String astn = ast.getRole2();
      Entity ent = ast.getEntity2();
      String ename = ent.getName();
      String par = ename + " " + astn + "x";   // or a list?
      if (previous)
      { res = res + ", " + par; }
      else
      { res = par;
        previous = true;
      }
    }
    return res;
  }

  public String getLookupParamValues()
  { String res = "";
    boolean previous = false;
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attn = att.getName();
      String par = attn + "x";
      if (previous)
      { res = res + ", " + par; }
      else
      { res = par;
        previous = true;
      }
    }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      String astn = ast.getRole2();
      String par = astn + "x";
      if (previous)
      { res = res + ", " + par; }
      else
      { res = par;
        previous = true;
      }
    }
    return res;
  }

  public String getStereotypesString()
  { String res = ""; 
    for (int i = 0; i < stereotypes.size(); i++) 
    { res = res + stereotypes.get(i) + " "; } 
    return res; 
  } 

  public BExpression getBEqualityPred(Vector vals)
  { // conjunction of att1(ex) = val1 & ...
    int attc = attributes.size();
    int astc = associations.size();
    int vc = vals.size();
    if (attc == 0 && astc == 0)
    { return new BBasicExpression("true"); }
    attc = (int) Math.min(attc,vc);
    String exname = getName().toLowerCase() + "x";
    BBasicExpression ex = new BBasicExpression(exname);
    BExpression pred = new BBasicExpression("true");
    boolean previous = false;
    for (int i = 0; i < attc; i++)
    { Attribute att = (Attribute) attributes.get(i);
      BExpression val = (BExpression) vals.get(i);
      String nme = att.getName();
      BExpression app =
        new BBinaryExpression("=",
          new BApplyExpression(nme,ex),val);
      if (previous)
      { pred = new BBinaryExpression("&",pred,app); }
      else
      { pred = app;
        previous = true;
      }
    }
    astc = (int) Math.min(astc,vc-attc);
    for (int i = 0; i < astc; i++)
    { Association ast = (Association) associations.get(i);
      BExpression val = (BExpression) vals.get(i+attc);
      String nme = ast.getRole2();
      BExpression app =
        new BBinaryExpression("=",
          new BApplyExpression(nme,ex),val);
      if (previous)
      { pred = new BBinaryExpression("&",pred,app); }
      else
      { pred = app;
        previous = true;
      }
    }
    return pred;
  }

  public String getEqualFieldsOp()
  { // conjunction of att1 = val1 & ...
    String res = "  public boolean equalFields(";
    res = res + getLookupParameters() + ")\n";
    int attc = attributes.size();
    int astc = associations.size();
    boolean previous = false;
    res = res + "  { return ";
    for (int i = 0; i < attc; i++)
    { Attribute att = (Attribute) attributes.get(i);
      String app = ""; 
      String nme = att.getName();
      Type typ = att.getType(); 
      String tname = typ.getName(); 
      if (tname.equals("String"))
      { app = nme + ".equals(" + nme + "x)"; } 
      else 
      { app = nme + " == " + nme + "x"; } 
       
      if (previous)
      { res = res + " && \n      " + app; }
      else
      { res = app;
        previous = true;
      }
    }
    for (int i = 0; i < astc; i++)
    { Association ast = (Association) associations.get(i);
      String nme = ast.getRole2();
      String app = nme + ".equals(" + nme + "x)";
      if (previous)
      { res = res + " && \n      " + app; }
      else
      { res = app;
        previous = true;
      }
    }
    return res + ";\n  }";
  }

  public static String getRecursiveManyOps(String role,
                                           String ename)
  { // for self-relations on an entity, role is MANY
    String res = "  public static List getRecursive" +
      role + "(" +
      ename + " _oo, List _seen)\n" +
      "  { if (_seen.contains(_oo)) { return _seen; }\n" +
      "    _seen.add(_oo);\n" +
      "    List _rs = _oo.get" + role + "();\n" +
      "    for (int _i = 0; _i < rs.size(); _i++)\n" +
      "    { " + ename + " _oo2 = (" + ename +
             ") _rs.get(_i);\n" +
      "      getRecursive" + role + "(_oo2,_seen);\n" +
      "    }\n" +
      "    return _seen;\n" +
      "  }\n\n";
    res = res + " public static List getRecursiveInverse" +
      role + "(" + ename + " _oo," +
      "List _entities, List _seen)\n" +
      "  { if (_seen.contains(_oo)) { return _seen; }\n" +
      "    _seen.add(_oo);\n" +
      "    for (int _i = 0; _i < _entities.size(); _i++)\n" +
      "    { " + ename + " _oo2 = (" + ename +
      ") _entities.get(_i);\n" +
      "    if (_oo2.get" + role + "().contains(_oo))\n" +
      "    { getRecursiveInverse" + role +
      "(_oo2,_entities,_seen); }\n" +
      "  }\n" +
      "  return _seen; }\n\n";
    return res;
  } // And recursiveOneOps

  public static boolean isAncestor(Entity a, Entity b)
  { if (b == null) { return false; } 

    Entity s = b.getSuperclass();
    if (s == null) { return false; }
    if (s == a) { return true; }   // s.equals(a) ?? 
    return isAncestor(a,s);
  }

  public static boolean inheritanceRelated(Entity a, Entity b)
  { if (a == null) 
    { return false; } 
    if (b == null) 
    { return false; } 
    if (a == b) 
    { return true; } 
    if (isAncestor(a,b)) { return true; } 
    if (isAncestor(b,a)) { return true; } 
    return false; 
  } 


  public static Entity commonSuperclass(Entity e1,
                                        Entity e2)
  { if (e1 == e2) 
    { return e1; } 
    if (isAncestor(e1,e2))
    { return e1; }
    if (isAncestor(e2,e1))
    { return e2; }
    Entity es1 = e1.getSuperclass();
    while (es1 != null)
    { if (isAncestor(es1,e2))
      { return es1; }
      es1 = es1.getSuperclass();
    }

    // System.err.println("No common superclass of: " + e1 + " and " + e2); 
    return null;
  }

  public static boolean haveCommonSuperclass(Vector tsources)
  { boolean res = true; 
    for (int j = 0; j < tsources.size() - 1; j++) 
    { Entity s1 = (Entity) tsources.get(j); 
      Entity s2 = (Entity) tsources.get(j+1); 
      if (Entity.commonSuperclass(s1,s2) != null) { } 
      else 
      { return false; } 
    } 
    return res; 
  } 

  public static Entity leastCommonSuperclass(Vector ents) 
  { // result is closest ancestor of all of ents
    if (ents.size() == 0) 
    { return null; } 
    if (ents.size() == 1) 
    { return (Entity) ents.get(0); } 
    Entity first = (Entity) ents.get(0); 
    Vector remainder = new Vector(); 
    remainder.addAll(ents); 
    remainder.remove(first); 
    Entity rest = leastCommonSuperclass(remainder); 
    return commonSuperclass(first,rest); 
  } 

  public static Map extendMapToAbstractClasses(Map mm, Vector allclasses, Vector concreteclasses) 
  { Vector newmaplets = new Vector(); 

    for (int i = 0; i < allclasses.size(); i++) 
    { Entity abse = (Entity) allclasses.get(i); 
      if (concreteclasses.contains(abse)) { } 
      else
      { Entity tent = abse.deriveAbstractClassMatching(mm); 
        if (tent != null) 
        { Maplet newmap = new Maplet(abse,tent); 
          newmaplets.add(newmap); 
        } 
      } 
    }

    if (newmaplets.size() > 0) 
    { Map res = new Map(mm); 
      res.addAll(newmaplets); 
      return res; 
    } 
    return mm; 
  }  
  
  public Entity deriveAbstractClassMatching(Map mm) 
  { // ents = all leaf subclasses of this; find their mappings under mm, 
    // and take least common superclass of those mappings (if any). 

    Vector tleafs = new Vector(); 

    for (int i = 0; i < allLeafSubclasses.size(); i++) 
    { Entity eleaf = (Entity) allLeafSubclasses.get(i); 
      Entity tleaf = (Entity) mm.get(eleaf); 
      if (tleaf != null) 
      { tleafs.add(tleaf); } 
    } 
    Entity res = leastCommonSuperclass(tleafs); 
    return res; 
  } 

  public static boolean comparable(Entity e1, Entity e2)
  { // if intersection of e1 and e2 is non-empty in principle
    return e1 == e2 || isAncestor(e1,e2) || isAncestor(e2,e1); 
  } // assuming no multiple inheritance. 

  public BExpression searchForSubclass(java.util.Map env) 
  { // search subclass tree in level order: 
    for (int i = 0; i < subclasses.size(); i++) 
    { Entity sub = (Entity) subclasses.get(i); 
      BExpression var = (BExpression) env.get(sub.getName()); 
      if (var != null) 
      { return var; } 
    } 
    // look at next level down: 
    for (int j = 0; j < subclasses.size(); j++) 
    { Entity sub = (Entity) subclasses.get(j); 
      BExpression var = sub.searchForSubclass(env); 
      if (var != null)
      { return var; }
    }
    return null; 
  } 

  public Entity searchForSubclassWithAttribute(String nme) 
  { // search subclass tree in level order: 
    for (int i = 0; i < subclasses.size(); i++) 
    { Entity sub = (Entity) subclasses.get(i); 
      if (sub.hasAttribute(nme))
      { return sub; } 
    } 
    // look at next level down: 
    for (int j = 0; j < subclasses.size(); j++) 
    { Entity sub = (Entity) subclasses.get(j); 
      Entity subsub = sub.searchForSubclassWithAttribute(nme); 
      if (subsub != null)
      { return subsub; }
    }
    return null; 
  } 

  public Entity searchForSubclassWithRole(String nme) 
  { // search subclass tree in level order: 
    for (int i = 0; i < subclasses.size(); i++) 
    { Entity sub = (Entity) subclasses.get(i); 
      if (sub.hasRole(nme))
      { return sub; } 
    } 
    // look at next level down: 
    for (int j = 0; j < subclasses.size(); j++) 
    { Entity sub = (Entity) subclasses.get(j); 
      Entity subsub = sub.searchForSubclassWithRole(nme); 
      if (subsub != null)
      { return subsub; }
    }
    return null; 
  } 

  public Entity searchForSubclassWithOperation(String nme) 
  { // search subclass tree in level order: 
    for (int i = 0; i < subclasses.size(); i++) 
    { Entity sub = (Entity) subclasses.get(i); 
      if (sub.getOperation(nme) != null)
      { return sub; } 
    } 
    // look at next level down: 
    for (int j = 0; j < subclasses.size(); j++) 
    { Entity sub = (Entity) subclasses.get(j); 
      Entity subsub = sub.searchForSubclassWithOperation(nme); 
      if (subsub != null)
      { return subsub; }
    }
    return null; 
  } 


  public String searchForSubclassJava(java.util.Map env)
  { // search subclass tree in level order: 
    for (int i = 0; i < subclasses.size(); i++)
    { Entity sub = (Entity) subclasses.get(i);
      String var = (String) env.get(sub.getName());
      if (var != null)
      { return var; }
    }
    // look at next level down: 
    for (int j = 0; j < subclasses.size(); j++)
    { Entity sub = (Entity) subclasses.get(j);
      String var = sub.searchForSubclassJava(env);
      if (var != null)
      { return var; }
    }
    return null;
  }

  public BExpression searchForSuperclass(java.util.Map env)
  { // search superclass tree in level order: 
    if (superclass != null)
    { BExpression var = (BExpression) env.get(superclass.getName());
      if (var != null)
      { return var; }
      else // look at next level up: 
      { return superclass.searchForSuperclass(env); }
    }
    return null;
  }

  public String searchForSuperclassJava(java.util.Map env)
  { // search superclass tree in level order: 
    if (superclass != null)
    { String var = (String) env.get(superclass.getName());
      if (var != null)
      { return var; }
      else // look at next level up: 
      { return superclass.searchForSuperclassJava(env); }
    }
    return null;
  }

  public String buildCreatePkOp(Attribute att)
  { // att is a primary or unique key of this class
    if (isAbstract() || isInterface()) { return ""; }

    String ename = getName();
    String ex = ename.toLowerCase() + "x";
    String attname = att.getName();
    String atype = att.getType().getJava();
    String attx = attname + "x";

    String header = "  public " + ename + " create" + ename + attname + "(" + atype + " " + attx + ")\n" +
      "  { " + ename + " " + ex + " = null;\n" +
      att.getUniqueCheckCode(this, ename) + "\n" +
      "    " + ex + " = new " + ename + "();\n" +
      "    add" + ename + "(" + ex + ");\n" +
      att.getCreateCode(this,ex) +
      att.getUniqueUpdateCode(this,ename) +
      "    return " + ex + ";\n" +
      "  }\n\n";
    return header;
  }
 
  public String buildCreateOp0()
  { // att is a primary or unique key of this class
    if (isAbstract() || isInterface()) { return ""; }

    String ename = getName();
    String ex = ename.toLowerCase() + "x";

    String header = "  public " + ename + " create" + ename + "()\n" +
      "  { " + ename + " " + ex + " = new " + ename + "();\n" +
      "    add" + ename + "(" + ex + ");\n" +
      "    return " + ex + ";\n" +
      "  }\n\n";
    return header;
  }
 
  public String interfaceCreateOp()
  { if (isAbstract() || isInterface()) { return ""; } 
    String ename = getName();
    String ex = ename.toLowerCase() + "x";
    String header = "  public " + ename + " create" + ename +
                 "(" + createOpParameters() + ");\n";
    return header; 
  }

  public String buildCreateOp(Vector cons, Vector entities, Vector types)
  { if (isAbstract() || isInterface()) { return ""; } 
    String ename = getName();
    String ex = ename.toLowerCase() + "x";
    String es = ename.toLowerCase() + "s";
    String tests = ""; 
    String upds = ""; 
    String header = "  public " + ename + " create" + ename +
                 "(" + createOpParameters() + ")\n";
    String inits = ""; 

    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      tests = tests + att.getUniqueCheckCode(this,ename);
      inits = inits + att.getCreateCode(this,ex);
      upds = upds + att.getUniqueUpdateCode(this,ename); 
    }

    String cardcheck = cardinalityCheckCode(es); 
    String res = createAllOp(ename,ex) + "\n" + 
                 header + "  { " + ename + " " + ex + ";\n" + 
                 cardcheck + tests + "    " + 
                 ex + " = new " + ename + "(" + createOpArgs() + ");\n";
    res = res + "    add" + ename + "(" + ex + ");\n";
    res = res + inits; 
    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      res = res + ast.getCreateCode(ex);
    }
    res = res + upds + 
    createActions(ex,ename,cons,entities,types) + 
    "\n    return " + ex + ";\n"; 
    return res + "  }\n\n";
  }

  private String createActions(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("create" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchCreate(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateForm(env,false); }
      }
    }
    return res;
  }

  private String createActionsJava6(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("create" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchCreate(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormJava6(env,false); }
      }
    }
    return res;
  }

  private String createActionsJava7(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("create" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchCreate(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormJava7(env,false); }
      }
    }
    return res;
  }

  private String createActionsCSharp(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("create" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchCreate(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormCSharp(env,false); }
      }
    }
    return res;
  }

  private String createActionsCPP(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("create" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchCreate(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormCPP(env,false); }
      }
    }
    return res;
  }


  private String createOpParameters()
  { // T att for each frozen or unique att, Entity2 role2 each frozen or ONE
    String res = "";
    boolean previous = false;
    Vector pars = new Vector(); 

    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isUnique() || att.isFrozen() && !att.isFinal())  // not frozen
      { String t = att.getType().getJava();
        String attn = att.getName();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + attn + "x";
        pars.add(attn); 
        previous = true;
      }
    }

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (pars.contains(atind.getName())) { }
      else 
      { if (previous)
        { res = res + ", "; } 
        res = res + atind.getType().getJava() + " " + atind.getName() + "x"; 
        previous = true; 
      } 
    } 

    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      if (ast.isQualified()) { continue; } 

      if (ast.getCard2() == ONE)
      { String t = ast.getEntity2().getName();
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
      else if (ast.isFrozen())
      { String t = "List";
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
    }
    return res;
  }

  private String createOpParametersJava6()
  { // T att for each frozen or unique att, Entity2 role2 each frozen or ONE
    String res = "";
    boolean previous = false;
    Vector pars = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isUnique() || att.isFrozen() && !att.isFinal())  // not frozen
      { String t = att.getType().getJava6();
        String attn = att.getName();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + attn + "x";
        pars.add(attn); 
        previous = true;
      }
    }

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (pars.contains(atind.getName())) { }
      else 
      { if (previous)
        { res = res + ", "; } 
        res = res + atind.getType().getJava6() + " " + atind.getName() + "x"; 
        previous = true;
      }  
    } 

    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      if (ast.isQualified()) { continue; } 

      if (ast.getCard2() == ONE)
      { String t = ast.getEntity2().getName();
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
      else if (ast.isFrozen())
      { String t = "ArrayList";
        if (ast.isOrdered()) { } else { t = "HashSet"; } 
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
    }
    return res;
  }

  private String createOpParametersJava7()
  { // T att for each frozen or unique att, Entity2 role2 each frozen or ONE
    String res = "";
    boolean previous = false;
    Vector pars = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isUnique() || att.isFrozen() && !att.isFinal())  // not frozen
      { String t = att.getType().getJava7();
        String attn = att.getName();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + attn + "x";
        pars.add(attn); 
        previous = true;
      }
    }

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (pars.contains(atind.getName())) { }
      else 
      { if (previous)
        { res = res + ", "; } 
        res = res + atind.getType().getJava7() + " " + atind.getName() + "x"; 
        previous = true;
      }  
    } 

    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      if (ast.isQualified()) { continue; } 
  
      String e2name = ast.getEntity2().getName(); 

      if (ast.getCard2() == ONE)
      { String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + e2name + " " + astn + "x";
        previous = true;
      }
      else if (ast.isFrozen())
      { String t = "ArrayList<" + e2name + ">";
        if (ast.isOrdered()) { } 
        else if (ast.isSorted())
        { t = "TreeSet<" + e2name + ">"; }
        else 
        { t = "HashSet<" + e2name + ">"; } 
 
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
    }
    return res;
  }

  private String createOpArgs()  // and for identity ones? - they are frozen? 
  { String res = "";
    boolean previous = false;
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isFrozen() && !att.isFinal())
      { String attn = att.getName();
        if (previous)
        { res = res + ","; }
        res = res + attn + "x";
        previous = true;
      }
    }
    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      if (ast.isQualified()) { continue; } 

      if (ast.getCard2() == ONE || ast.isFrozen())
      { String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + astn + "x";
        previous = true;
      }
    }
    return res;
  }

  public String buildCreateOpJava6(Vector cons, Vector entities, Vector types)
  { if (isAbstract() || isInterface()) { return ""; } 
    String ename = getName();
    String ex = ename.toLowerCase() + "x";
    String es = ename.toLowerCase() + "s";
    String tests = ""; 
    String upds = ""; 
    String header = "  public " + ename + " create" + ename +
                 "(" + createOpParametersJava6() + ")\n";
    String inits = ""; 

    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      tests = tests + att.getUniqueCheckCode(this,ename);
      inits = inits + att.getCreateCodeJava6(this,ex);
      upds = upds + att.getUniqueUpdateCode(this,ename); 
    }

    String cardcheck = cardinalityCheckCode(es); 
    String res = createAllOpJava6(ename,ex) + "\n" + 
                 header + "  { " + ename + " " + ex + ";\n" + 
                 cardcheck + tests + "    " + 
                 ex + " = new " + ename + "(" + createOpArgs() + ");\n";
    res = res + "    add" + ename + "(" + ex + ");\n";
    res = res + inits; 
    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      res = res + ast.getCreateCodeJava6(ex);
    }
    res = res + upds + 
    createActionsJava6(ex,ename,cons,entities,types) + 
    "\n    return " + ex + ";\n"; 
    return res + "  }\n\n";
  }

  public String buildCreateOpJava7(Vector cons, Vector entities, Vector types)
  { if (isAbstract() || isInterface()) { return ""; } 
    String ename = getName();
    String ex = ename.toLowerCase() + "x";
    String es = ename.toLowerCase() + "s";
    String tests = ""; 
    String upds = ""; 
    String header = "  public " + ename + " create" + ename +
                 "(" + createOpParametersJava7() + ")\n";
    String inits = ""; 

    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      tests = tests + att.getUniqueCheckCode(this,ename);
      inits = inits + att.getCreateCodeJava7(this,ex);
      upds = upds + att.getUniqueUpdateCode(this,ename); 
    }

    String cardcheck = cardinalityCheckCode(es); 
    String res = createAllOpJava7(ename,ex) + "\n" + 
                 header + 
                 "  { " + ename + " " + ex + ";\n" + 
                 cardcheck + tests + "    " + 
                 "    " + ex + " = new " + ename + "(" + createOpArgs() + ");\n";
    res = res + "    add" + ename + "(" + ex + ");\n";
    res = res + inits; 
    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      res = res + ast.getCreateCodeJava7(ex);
    }
    res = res + upds + 
    createActionsJava7(ex,ename,cons,entities,types) + 
    "\n    return " + ex + ";\n"; 
    return res + "  }\n\n";
  }

  public String buildCreateOpCSharp(Vector cons, Vector entities, Vector types)
  { if (isAbstract() || isInterface()) { return ""; } 
    String ename = getName();
    String ex = ename.toLowerCase() + "x";
    String es = ename.toLowerCase() + "_s";
    String tests = ""; 
    String upds = ""; 
    String header = "  public " + ename + " create" + ename +
                 "(" + createOpParametersCSharp() + ")\n";
    String inits = ""; 

    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      tests = tests + att.getUniqueCheckCodeCSharp(this,ename);
      inits = inits + att.getCreateCodeCSharp(this,ex);
      upds = upds + att.getUniqueUpdateCodeCSharp(this,ename); 
    }
    String cardcheck = cardinalityCheckCodeCSharp(es); 
    String res = createAllOpCSharp(ename,ex) + "\n" + 
                 header + "  { \n" + cardcheck + tests + "    " + 
                 ename + " " + ex + " = new " +
                 ename + "(" + createOpArgs() + ");\n";
    res = res + "    add" + ename + "(" + ex + ");\n";
    res = res + inits; 
    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      res = res + ast.getCreateCodeCSharp(ex);
    }
    res = res + upds + 
    createActionsCSharp(ex,ename,cons,entities,types) + 
    "\n    return " + ex + ";\n"; 
    return res + "  }\n\n";
  }


  private String createOpParametersCSharp()
  { // T att for each frozen or unique att, Entity2 role2 each frozen or ONE
    String res = "";
    boolean previous = false;
    Vector pars = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isUnique() || att.isFrozen() && !att.isFinal())  // not frozen
      { String t = att.getType().getCSharp();
        String attn = att.getName();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + attn + "x";
        pars.add(attn); 
        previous = true;
      }
    }

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (pars.contains(atind.getName())) { } 
      else 
      { if (previous)
        { res = res + ", "; } 
        res = res + atind.getType().getCSharp() + " " + atind.getName() + "x"; 
        previous = true;
      }  
    } 

    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      if (ast.isQualified()) { continue; } 

      if (ast.getCard2() == ONE)
      { String t = ast.getEntity2().getName();
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
      else if (ast.isFrozen())
      { String t = "ArrayList";
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
    }
    return res;
  }

  private String createOpParametersCPP()
  { // T att for each frozen or unique att, Entity2* role2 each frozen or ONE
    String res = "";
    boolean previous = false;
    Vector pars = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (att.isUnique() || att.isFrozen() && !att.isFinal())  // not frozen
      { String t = att.getType().getCPP();
        String attn = att.getName();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + attn + "x";
        pars.add(attn); 
        previous = true;
      }
    }

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (pars.contains(atind.getName())) { }
      else 
      { if (previous)
        { res = res + ", "; } 
        res = res + atind.getType().getCPP() + " " + atind.getName() + "x"; 
        previous = true;
      }  
    } 

    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      if (ast.isQualified()) { continue; } 

      if (ast.getCard2() == ONE)
      { String t = ast.getEntity2().getName();
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + "* " + astn + "x";
        previous = true;
      }
      else if (ast.isFrozen())
      { String t = "<" + ast.getEntity2().getName() + "*>*";
        if (ast.isOrdered()) 
        { t = "vector" + t; } 
        else 
        { t = "set" + t; } 
        String astn = ast.getRole2();
        if (previous)
        { res = res + ","; }
        res = res + t + " " + astn + "x";
        previous = true;
      }
    }
    return res;
  }

  public String buildCreateOpCPP(Vector cons, Vector entities, Vector types)
  { if (isAbstract() || isInterface()) { return ""; } 
    String ename = getName();
    String ex = ename.toLowerCase() + "x";
    String es = ename.toLowerCase() + "_s";
    String tests = ""; 
    String upds = ""; 
    String header = "  " + ename + "* create" + ename +
                 "(" + createOpParametersCPP() + ")\n";
    String inits = ""; 

    Vector allatts = new Vector(); 

    Vector vec = new Vector(); 
    Attribute atind = getPrincipalUK(this,vec); 
    if (atind != null && this == (Entity) vec.get(0))  // a key defined as unique in this class
    { if (attributes.contains(atind)) { } 
      else { allatts.add(atind); }
    }  
    allatts.addAll(attributes); 

    for (int i = 0; i < allatts.size(); i++)
    { Attribute att = (Attribute) allatts.get(i);
      tests = tests + att.getUniqueCheckCodeCPP(this,ename);
      inits = inits + att.getCreateCodeCPP(this,ex);
      upds = upds + att.getUniqueUpdateCodeCPP(this,ename); 
    }
    String cardcheck = cardinalityCheckCode(es); 
    String res = createAllOpCPP(ename,ex) + "\n" + 
                 header + "  { \n" + cardcheck + tests + "    " + 
                 ename + "* " + ex + " = new " +
                 ename + "(" + createOpArgs() + ");\n";
    res = res + "    add" + ename + "(" + ex + ");\n";
    res = res + inits; 
    for (int j = 0; j < associations.size(); j++)
    { Association ast = (Association) associations.get(j);
      res = res + ast.getCreateCodeCPP(ex);
    }
    res = res + upds + 
    createActionsCPP(ex,ename,cons,entities,types) + 
    "\n    return " + ex + ";\n"; 
    return res + "  }\n\n";
  }


  public String buildDeleteOp(Vector assocs, Vector cons, Vector entities, Vector types)
  { String ename = getName(); 
    return buildKillOp(ename,superclass,assocs, cons, entities, types); 
  } 

  public String buildDeleteOpJava6(Vector assocs, Vector cons, Vector entities, Vector types)
  { String ename = getName(); 
    return buildKillOpJava6(ename,superclass,assocs, cons, entities, types); 
  } 

  public String buildDeleteOpJava7(Vector assocs, Vector cons, Vector entities, Vector types)
  { String ename = getName(); 
    return buildKillOpJava7(ename,superclass,assocs, cons, entities, types); 
  } 

  public String buildDeleteOpCSharp(Vector assocs, Vector cons, Vector entities, Vector types)
  { String ename = getName(); 
    return buildKillOpCSharp(ename,superclass,assocs, cons, entities, types); 
  } 

  public String buildDeleteOpCPP(Vector assocs, Vector cons, Vector entities, Vector types)
  { String ename = getName(); 
    return buildKillOpCPP(ename,superclass,assocs, cons, entities, types); 
  } 


  public String interfaceKillOp()
  { String ename = getName(); 
    String ex = ename.toLowerCase() + "xx";
    String res = "  public void kill" + ename +
                 "(" + ename + " " + ex + ");\n";
    if (isAbstract())
    { res = res + "  public void killAbstract" + ename + "(" + ename + " " + ex +
                  ");\n\n" + 
                  "  public void killAbstract" + ename + "(List " + ex + ");\n\n"; 
    } 
    return res;
  }

  public String interfaceKillOpJava6()
  { String ename = getName(); 
    String ex = ename.toLowerCase() + "xx";
    String res = "  public void kill" + ename +
                 "(" + ename + " " + ex + ");\n";
    if (isAbstract())
    { res = res + "  public void killAbstract" + ename + "(" + ename + " " + ex +
                  ");\n\n" + 
                  "  public void killAbstract" + ename + "(Collection " + ex + ");\n\n"; 
    } 
    return res;
  }

  public String interfaceKillOpJava7()
  { String ename = getName(); 
    String ex = ename.toLowerCase() + "xx";
    String res = "  public void kill" + ename +
                 "(" + ename + " " + ex + ");\n";
    if (isAbstract())
    { res = res + "  public void killAbstract" + ename + "(" + ename + " " + ex +
                  ");\n\n" + 
                  "  public void killAbstract" + ename + "(Collection<" + ename + "> " + ex + ");\n\n"; 
    } 
    return res;
  }

   

  private String buildKillOp(String ename, Entity sup, 
                             Vector assocs0, Vector cons, 
                             Vector entities, Vector types)
  { String ex = ename.toLowerCase() + "xx";
    String es = ename.toLowerCase() + "s";
    String precode = ""; 
    String endcode = "";
    String midcode = ""; 
    String res = killAllOp(ename,ex); 
    res = res + "  public void kill" + ename +
                 "(" + ename + " " + ex + ")\n";
    res = res + "  { if (" + ex + " == null) { return; }\n" + 
                "   " + es + ".remove(" + ex + ");\n";

    Vector assocs = new Vector(); 
    assocs.addAll(assocs0); 

    for (int i = 0; i < assocs.size(); i++)
    { Association ast = (Association) assocs.get(i);
      Entity e2 = ast.getEntity2();
      Entity e1 = ast.getEntity1();
      String role2 = ast.getRole2(); 
      String role1 = ast.getRole1(); 
      int c1 = ast.getCard1(); 
      int c2 = ast.getCard2(); 

      // Associations with this as role2 (MANY/ZEROONE) simply 
      // remove ex from role2
      // Assocations with this as role2 (ONE) kill all e1x linked to ex
      // Aggregations (ZEROONE or ONE at role2) likewise
     
      if (e2.getName().equals(ename))
      { String e1name = e1.getName();
        String e1s = e1name.toLowerCase() + "s";

        String abs = "";
        if (e1.isAbstract())
        { abs = "Abstract"; } 

        if (c2 == ModelElement.ONE)  // Kill all attached e1x's
        { precode = precode + "    Vector _1removed" + role2 + e1name + " = " + 
                    "new Vector();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _1removed" + role2 + e1name +
            ".size(); _i++)\n" + 
            "    { kill" + abs + e1name + "((" +
            e1name + ") _1removed" + role2 + e1name +
            ".get(_i)); }\n";
        }
        String e1del = ast.getKillCode(ex,e1s);
        midcode = midcode + e1del;
      }
      
      if (e1.getName().equals(ename) && ast.isAggregation())
      { // delete all attached e2s:
        String e2name = e2.getName(); 

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        precode = precode + "    Vector _deleted" + role2 + e2name + " = " + 
                    "new Vector();\n"; 
        endcode = endcode + "    " +
            "for (int _i = 0; _i < _deleted" + role2 + e2name +
            ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _deleted" + role2 + e2name +
            ".get(_i)); }\n";
        
        if (c2 != ModelElement.ONE) 
        { String e2del = "  _deleted" + role2 + e2name + 
                         ".addAll(" + ex + ".get" + role2 + "());\n" + 
                         "    " + ex + ".set" + role2 + "(new Vector());\n";
          midcode = midcode + e2del;
        } // ex.setrole2(new Vector()) is not necessary logically. 
        else 
        { String e2del = 
            "  if (" + ex + ".get" + role2 + "() != null)\n" + 
            "  { _deleted" + role2 + e2name + ".add(" + ex + ".get" + role2 + "()); }\n" + 
            "  " + ex + ".set" + role2 + "(null);\n";  // OK since ex /: es at end
          midcode = midcode + e2del;
        } 
      }
      else if (e1.getName().equals(ename) && role1 != null && role1.length() > 0)
      { String e2name = e2.getName();
        String e2s = e2name.toLowerCase() + "s";

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        if (c1 == ModelElement.ONE) // kill all attached e2x's 
        { precode = precode + "    Vector _2removed" + role1 + e2name + " = " + 
                    "new Vector();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _2removed" + role1 + e2name + ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _2removed" + role1 + e2name + ".get(_i)); }\n";
        } 
        midcode = midcode + ast.getDualKillCode(ex,e2s);
        if (c2 != ModelElement.ONE)  // Not logically needed
        { midcode = midcode + "\n    " + ex + ".set" + role2 + "(new Vector());\n";
}           
      } 
    }

    for (int k = 0; k < attributes.size(); k++)
    { Attribute att = (Attribute) attributes.get(k);
      if (att.isUnique())  // remove att from eattindex
      { String attnme = att.getName();
        String removeindex =  "    " + ename.toLowerCase() + attnme +
                              "index.remove(" + ex + ".get" + attnme +
                              "());\n";
        midcode = midcode + removeindex;
      }
    }

    if (sup != null) 
    { endcode = endcode + "  kill" + sup.getName() + "(" + ex + ");\n"; }  
    for (int i = 0; i < interfaces.size(); i++)
    { Entity intf = (Entity) interfaces.get(i); 
      endcode = endcode + "  kill" + intf.getName() + "(" + ex + ");\n";
    }  

    endcode = endcode + killActions(ex, ename, cons, entities, types); 
      
    res = res + precode + midcode + endcode + "  }\n\n";

    if (isAbstract())
    { Vector leafs = getActualLeafSubclasses(); 
      res = res + "  public void killAbstract" + ename + 
                  "(" + ename + " " + ex + ")\n";
      res = res + "  {\n"; 
      for (int ll = 0; ll < leafs.size(); ll++)
      { Entity lc = (Entity) leafs.get(ll); 
        res = res + "    if (" + ex + " instanceof " + lc + ")\n" + 
                    "    { kill" + lc + "((" + lc + ") " + ex + "); }\n"; 
      } 
      res = res + "  }\n\n"; 
      res = res + "  public void killAbstract" + ename + 
                  "(List _l)\n";
      res = res + "  { for (int _i = 0; _i < _l.size(); _i++)\n" + 
                  "    { " + ename + " _e = (" + ename + ") _l.get(_i);\n" + 
                  "      killAbstract" + ename + "(_e);\n" + 
                  "    }\n" + 
                  "  }\n\n"; 
    } 

    return res;
  }

  private String buildKillOpJava6(String ename, Entity sup, 
                             Vector assocs, Vector cons, 
                             Vector entities, Vector types)
  { String ex = ename.toLowerCase() + "xx";
    String es = ename.toLowerCase() + "s";
    String precode = ""; 
    String endcode = "";
    String midcode = ""; 
    String res = killAllOpJava6(ename,ex); 
    res = res + "  public void kill" + ename +
                 "(" + ename + " " + ex + ")\n";
    res = res + "  { " + es + ".remove(" + ex + ");\n";
    for (int i = 0; i < assocs.size(); i++)
    { Association ast = (Association) assocs.get(i);
      Entity e2 = ast.getEntity2();
      Entity e1 = ast.getEntity1();
      String role2 = ast.getRole2(); 
      String role1 = ast.getRole1(); 
      int c1 = ast.getCard1(); 
      int c2 = ast.getCard2(); 

      // Associations with this as role2 (MANY/ZEROONE) simply 
      // remove ex from role2
      // Assocations with this as role2 (ONE) kill all e1x linked to ex
      // Aggregations (ZEROONE or ONE at role2) likewise
     
      if (e2.getName().equals(ename))
      { String e1name = e1.getName();
        String e1s = e1name.toLowerCase() + "s";

        String abs = "";
        if (e1.isAbstract())
        { abs = "Abstract"; } 

        if (c2 == ModelElement.ONE)  // Kill all attached e1x's
        { precode = precode + "    ArrayList _1removed" + role2 + e1name + " = " + 
                    "new ArrayList();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _1removed" + role2 + e1name + ".size(); _i++)\n" + 
            "    { kill" + abs + e1name + "((" +
            e1name + ") _1removed" + role2 + e1name + ".get(_i)); }\n";
        }
        String e1del = ast.getKillCodeJava6(ex,e1s);
        midcode = midcode + e1del;
      }

      String rtype = ""; 

      if (ast.isOrdered())
      { rtype = "ArrayList"; } 
      else 
      { rtype = "HashSet"; } 
      
      if (e1.getName().equals(ename) && ast.isAggregation())
      { // delete all attached e2s:
        String e2name = e2.getName(); 

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        precode = precode + "    ArrayList _deleted" + role2 + e2name + " = " + 
                    "new ArrayList();\n"; 
        endcode = endcode + "    " +
            "for (int _i = 0; _i < _deleted" + role2 + e2name +
            ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _deleted" + role2 + e2name + ".get(_i)); }\n";
        
        if (c2 != ModelElement.ONE) 
        { String e2del = "  _deleted" + role2 + e2name + 
                         ".addAll(" + ex + ".get" + role2 + "());\n" + 
                         "    " + ex + ".set" + role2 + "(new " + rtype + "());\n";
          midcode = midcode + e2del;
        } // ex.setrole2(new Vector()) is not necessary logically. 
        else 
        { String e2del = 
            "  if (" + ex + ".get" + role2 + "() != null)\n" + 
            "  { _deleted" + role2 + e2name + ".add(" + ex + ".get" + role2 + "()); }\n" + 
            "  " + ex + ".set" + role2 + "(null);\n";  // OK since ex /: es at end
          midcode = midcode + e2del;
        } 
      }
      else if (e1.getName().equals(ename) && role1 != null && role1.length() > 0)
      { String e2name = e2.getName();
        String e2s = e2name.toLowerCase() + "s";

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        if (c1 == ModelElement.ONE) // kill all attached e2x's 
        { precode = precode + "    ArrayList _2removed" + role1 + e2name + " = " + 
                    "new ArrayList();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _2removed" + role1 + e2name + ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _2removed" + role1 + e2name + ".get(_i)); }\n";
        } 
        midcode = midcode + ast.getDualKillCodeJava6(ex,e2s);
        if (c2 != ModelElement.ONE)  // Not logically needed
        { midcode = midcode + "\n    " + ex + ".set" + role2 + "(new " + rtype + "());\n";
}           
      } 
    }

    for (int k = 0; k < attributes.size(); k++)
    { Attribute att = (Attribute) attributes.get(k);
      if (att.isUnique())  // remove att from eattindex
      { String attnme = att.getName();
        String removeindex =  "    " + ename.toLowerCase() + attnme +
                              "index.remove(" + ex + ".get" + attnme +
                              "());\n";
        midcode = midcode + removeindex;
      }
    }

    if (sup != null) 
    { endcode = endcode + "  kill" + sup.getName() + "(" + ex + ");\n"; }  
    for (int i = 0; i < interfaces.size(); i++)
    { Entity intf = (Entity) interfaces.get(i); 
      endcode = endcode + "  kill" + intf.getName() + "(" + ex + ");\n";
    }  

    endcode = endcode + killActionsJava6(ex, ename, cons, entities, types); 
      
    res = res + precode + midcode + endcode + "  }\n\n";

    if (isAbstract())
    { Vector leafs = getActualLeafSubclasses(); 
      res = res + "  public void killAbstract" + ename + 
                  "(" + ename + " " + ex + ")\n";
      res = res + "  {\n"; 
      for (int ll = 0; ll < leafs.size(); ll++)
      { Entity lc = (Entity) leafs.get(ll); 
        res = res + "    if (" + ex + " instanceof " + lc + ")\n" + 
                    "    { kill" + lc + "((" + lc + ") " + ex + "); }\n"; 
      } 
      res = res + "  }\n\n"; 
      res = res + "  public void killAbstract" + ename + 
                  "(Collection _l)\n";
      res = res + "  { for (Object _o : _l)\n" + 
                  "    { " + ename + " _e = (" + ename + ") _o;\n" + 
                  "      killAbstract" + ename + "(_e);\n" + 
                  "    }\n" + 
                  "  }\n\n"; 
    } 

    return res;
  }

  private String buildKillOpJava7(String ename, Entity sup, 
                             Vector assocs, Vector cons, 
                             Vector entities, Vector types)
  { String ex = ename.toLowerCase() + "xx";
    String es = ename.toLowerCase() + "s";
    String precode = ""; 
    String endcode = "";
    String midcode = ""; 
    String res = killAllOpJava7(ename,ex); 
    res = res + "  public void kill" + ename +
                 "(" + ename + " " + ex + ")\n";
    res = res + "  { " + es + ".remove(" + ex + ");\n";
    for (int i = 0; i < assocs.size(); i++)
    { Association ast = (Association) assocs.get(i);
      Entity e2 = ast.getEntity2();
      Entity e1 = ast.getEntity1();
      String role2 = ast.getRole2(); 
      String role1 = ast.getRole1(); 
      int c1 = ast.getCard1(); 
      int c2 = ast.getCard2(); 
      String e2name = e2.getName(); 

      // Associations with this as role2 (MANY/ZEROONE) simply 
      // remove ex from role2
      // Assocations with this as role2 (ONE) kill all e1x linked to ex
      // Aggregations (ZEROONE or ONE at role2) likewise
     
      if (e2.getName().equals(ename))
      { String e1name = e1.getName();
        String e1s = e1name.toLowerCase() + "s";

        String abs = "";
        if (e1.isAbstract())
        { abs = "Abstract"; } 

        if (c2 == ModelElement.ONE)  // Kill all attached e1x's
        { precode = precode + 
                    "    ArrayList<" + e1name + "> _1removed" + role2 + e1name + " = new ArrayList<" + e1name + ">();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _1removed" + role2 + e1name + ".size(); _i++)\n" + 
            "    { kill" + abs + e1name + "((" +
            e1name + ") _1removed" + role2 + e1name + ".get(_i)); }\n";
        }
        String e1del = ast.getKillCodeJava7(ex,e1s);
        midcode = midcode + e1del;
      }

      String rtype = ""; 

      if (ast.isOrdered())
      { rtype = "ArrayList<" + e2name + ">"; } 
      else if (ast.isSorted())
      { rtype = "TreeSet<" + e2name + ">"; } 
      else 
      { rtype = "HashSet<" + e2name + ">"; } 
      
      if (e1.getName().equals(ename) && ast.isAggregation())
      { // delete all attached e2s:

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        precode = precode + "    ArrayList<" + e2name + "> _deleted" + role2 + e2name + " = " + 
                    "new ArrayList<" + e2name + ">();\n"; 
        endcode = endcode + "    " +
            "for (int _i = 0; _i < _deleted" + role2 + e2name +
            ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _deleted" + role2 + e2name + ".get(_i)); }\n";
        
        if (c2 != ModelElement.ONE) 
        { String e2del = "  _deleted" + role2 + e2name + 
                         ".addAll(" + ex + ".get" + role2 + "());\n" + 
                         "    " + ex + ".set" + role2 + "(new " + rtype + "());\n";
          midcode = midcode + e2del;
        } // ex.setrole2(new Vector()) is not necessary logically. 
        else 
        { String e2del = 
            "  if (" + ex + ".get" + role2 + "() != null)\n" + 
            "  { _deleted" + role2 + e2name + ".add(" + ex + ".get" + role2 + "()); }\n" + 
            "  " + ex + ".set" + role2 + "(null);\n";  // OK since ex /: es at end
          midcode = midcode + e2del;
        } 
      }
      else if (e1.getName().equals(ename) && role1 != null && role1.length() > 0)
      { String e2s = e2name.toLowerCase() + "s";

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        if (c1 == ModelElement.ONE) // kill all attached e2x's 
        { precode = precode + "    ArrayList<" + e2name + "> _2removed" + role1 + e2name + " = " + 
                    "new ArrayList<" + e2name + ">();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _2removed" + role1 + e2name + ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _2removed" + role1 + e2name + ".get(_i)); }\n";
        } 
        midcode = midcode + ast.getDualKillCodeJava6(ex,e2s);
        if (c2 != ModelElement.ONE)  // Not logically needed
        { midcode = midcode + "\n    " + ex + ".set" + role2 + "(new " + rtype + "());\n";
}           
      } 
    }

    for (int k = 0; k < attributes.size(); k++)
    { Attribute att = (Attribute) attributes.get(k);
      if (att.isUnique())  // remove att from eattindex
      { String attnme = att.getName();
        String removeindex =  "    " + ename.toLowerCase() + attnme +
                              "index.remove(" + ex + ".get" + attnme +
                              "());\n";
        midcode = midcode + removeindex;
      }
    }

    if (sup != null) 
    { endcode = endcode + "  kill" + sup.getName() + "(" + ex + ");\n"; }  
    for (int i = 0; i < interfaces.size(); i++)
    { Entity intf = (Entity) interfaces.get(i); 
      endcode = endcode + "  kill" + intf.getName() + "(" + ex + ");\n";
    }  

    endcode = endcode + killActionsJava7(ex, ename, cons, entities, types); 
      
    res = res + precode + midcode + endcode + "  }\n\n";

    if (isAbstract())
    { Vector leafs = getActualLeafSubclasses(); 
      res = res + "  public void killAbstract" + ename + 
                  "(" + ename + " " + ex + ")\n";
      res = res + "  {\n"; 
      for (int ll = 0; ll < leafs.size(); ll++)
      { Entity lc = (Entity) leafs.get(ll); 
        res = res + "    if (" + ex + " instanceof " + lc + ")\n" + 
                    "    { kill" + lc + "((" + lc + ") " + ex + "); }\n"; 
      } 
      res = res + "  }\n\n"; 
      res = res + "  public void killAbstract" + ename + 
                  "(Collection<" + ename + "> _l)\n";
      res = res + "  { for (Object _o : _l)\n" + 
                  "    { " + ename + " _e = (" + ename + ") _o;\n" + 
                  "      killAbstract" + ename + "(_e);\n" + 
                  "    }\n" + 
                  "  }\n\n"; 
    } 

    return res;
  }

  private String buildKillOpCSharp(String ename, Entity sup, 
                             Vector assocs, Vector cons, 
                             Vector entities, Vector types)
  { String ex = ename.toLowerCase() + "xx";
    String es = ename.toLowerCase() + "_s";
    String precode = ""; 
    String endcode = "";
    String midcode = ""; 
    String res = killAllOpCSharp(ename,ex); 
    res = res + "  public void kill" + ename +
                 "(" + ename + " " + ex + ")\n";
    res = res + "  { " + es + ".Remove(" + ex + ");\n";
    for (int i = 0; i < assocs.size(); i++)
    { Association ast = (Association) assocs.get(i);
      Entity e2 = ast.getEntity2();
      Entity e1 = ast.getEntity1();
      String role2 = ast.getRole2(); 
      String role1 = ast.getRole1(); 
      int c1 = ast.getCard1(); 
      int c2 = ast.getCard2(); 

      // Associations with this as role2 (MANY/ZEROONE) simply 
      // remove ex from role2
      // Assocations with this as role2 (ONE) kill all e1x linked to ex
      // Aggregations (ZEROONE or ONE at role2) likewise
     
      if (e2.getName().equals(ename))
      { String e1name = e1.getName();
        String e1s = e1name.toLowerCase() + "_s";

        String abs = "";
        if (e1.isAbstract())
        { abs = "Abstract"; } 

        if (c2 == ModelElement.ONE)  // Kill all attached e1x's
        { precode = precode + "    ArrayList _1removed" + role2 + e1name + " = " + 
                    "new ArrayList();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _1removed" + role2 + e1name +
            ".Count; _i++)\n" + 
            "    { kill" + abs + e1name + "((" +
            e1name + ") _1removed" + role2 + e1name + "[_i]); }\n";
        }
        String e1del = ast.getKillCodeCSharp(ex,e1s);
        midcode = midcode + e1del;
      }
      
      if (e1.getName().equals(ename) && ast.isAggregation())
      { // delete all attached e2s:
        String e2name = e2.getName(); 

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        precode = precode + "    ArrayList _deleted" + role2 + e2name + " = " + 
                    "new ArrayList();\n"; 
        endcode = endcode + "    " +
            "for (int _i = 0; _i < _deleted" + role2 + e2name +
            ".Count; _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _deleted" + role2 + e2name + "[_i]); }\n";
        
        if (c2 != ModelElement.ONE) 
        { String e2del = "  _deleted" + role2 + e2name + 
                         ".AddRange(" + ex + ".get" + role2 + "());\n" + 
                         "    " + ex + ".set" + role2 + "(new ArrayList());\n";
          midcode = midcode + e2del;
        } // ex.setrole2(new Vector()) is not necessary logically. 
        else 
        { String e2del = 
            "  if (" + ex + ".get" + role2 + "() != null)\n" + 
            "  { _deleted" + role2 + e2name + ".Add(" + ex + ".get" + role2 + "()); }\n" + 
            "  " + ex + ".set" + role2 + "(null);\n";  // OK since ex /: es at end
          midcode = midcode + e2del;
        } 
      }
      else if (e1.getName().equals(ename) && role1 != null && role1.length() > 0)
      { String e2name = e2.getName();
        String e2s = e2name.toLowerCase() + "_s";

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        if (c1 == ModelElement.ONE) // kill all attached e2x's 
        { precode = precode + "    ArrayList _2removed" + role1 + e2name + " = " + 
                    "new ArrayList();\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < _2removed" + role1 + e2name + ".Count; _i++)\n" + 
            "    { kill" + abs2 + e2name + "((" +
            e2name + ") _2removed" + role1 + e2name + "[_i]); }\n";
        } 
        midcode = midcode + ast.getDualKillCodeCSharp(ex,e2s);
        if (c2 != ModelElement.ONE)  // Not logically needed
        { midcode = midcode + "\n    " + ex + ".set" + role2 + "(new ArrayList());\n";
}           
      } 
    }

    for (int k = 0; k < attributes.size(); k++)
    { Attribute att = (Attribute) attributes.get(k);
      if (att.isUnique())  // remove att from eattindex
      { String attnme = att.getName();
        String removeindex =  "    " + ename.toLowerCase() + attnme +
                              "index.Remove(" + ex + ".get" + attnme +
                              "());\n";
        midcode = midcode + removeindex;
      }
    }

    if (sup != null) 
    { endcode = endcode + "  kill" + sup.getName() + "(" + ex + ");\n"; }  
    for (int i = 0; i < interfaces.size(); i++)
    { Entity intf = (Entity) interfaces.get(i); 
      endcode = endcode + "  kill" + intf.getName() + "(" + ex + ");\n";
    }  

    endcode = endcode + killActionsCSharp(ex, ename, cons, entities, types); 
      
    res = res + precode + midcode + endcode + "  }\n\n";

    if (isAbstract())
    { Vector leafs = getActualLeafSubclasses(); 
      res = res + "  public void killAbstract" + ename + 
                  "(" + ename + " " + ex + ")\n";
      res = res + "  {\n"; 
      for (int ll = 0; ll < leafs.size(); ll++)
      { Entity lc = (Entity) leafs.get(ll); 
        res = res + "    if (" + ex + " is " + lc + ")\n" + 
                    "    { kill" + lc + "((" + lc + ") " + ex + "); }\n"; 
      } 
      res = res + "  }\n\n"; 
      res = res + "  public void killAbstract" + ename + 
                  "(ArrayList _l)\n";
      res = res + "  { for (int _i = 0; _i < _l.Count; _i++)\n" + 
                  "    { " + ename + " _e = (" + ename + ") _l[_i];\n" + 
                  "      killAbstract" + ename + "(_e);\n" + 
                  "    }\n" + 
                  "  }\n\n"; 
    } 

    return res;
  }

  private String buildKillOpCPP(String ename, Entity sup, 
                             Vector assocs, Vector cons, 
                             Vector entities, Vector types)
  { String ex = ename.toLowerCase() + "xx";
    String es = ename.toLowerCase() + "_s";
    String precode = ""; 
    String endcode = "";
    String midcode = ""; 
    String res = killAllOpCPP(ename,ex); 
    res = res + "  void kill" + ename +
                 "(" + ename + "* " + ex + ")\n";
    res = res + "  { " + es + "->erase(find(" + es + "->begin(), " + es + "->end(), " + ex + "));\n";
    for (int i = 0; i < assocs.size(); i++)
    { Association ast = (Association) assocs.get(i);
      Entity e2 = ast.getEntity2();
      Entity e1 = ast.getEntity1();
      String role2 = ast.getRole2(); 
      String role1 = ast.getRole1(); 
      int c1 = ast.getCard1(); 
      int c2 = ast.getCard2(); 

      // Associations with this as role2 (MANY/ZEROONE) simply 
      // remove ex from role2
      // Assocations with this as role2 (ONE) kill all e1x linked to ex
      // Aggregations (ZEROONE or ONE at role2) likewise
     
      if (e2.getName().equals(ename))
      { String e1name = e1.getName();
        String e1s = e1name.toLowerCase() + "_s";

        String abs = "";
        if (e1.isAbstract())
        { abs = "Abstract"; } 

        if (c2 == ModelElement.ONE)  // Kill all attached e1x's
        { String removede1s = "_1removed" + role2 + e1name; 
          precode = precode + "    vector<" + e1name + "*> " + removede1s + ";\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < " + removede1s + ".size(); _i++)\n" + 
            "    { kill" + abs + e1name + "(" +  removede1s + "[_i]); }\n";
        }
        String e1del = ast.getKillCodeCPP(ex,e1s);
        midcode = midcode + e1del;
      }
      
      if (e1.getName().equals(ename) && ast.isAggregation())
      { // delete all attached e2s:
        String e2name = e2.getName(); 

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        String deletede2s = "_deleted" + role2 + e2name; 
        precode = precode + "    vector<" + e2name + "*> " + deletede2s + ";\n"; 
        endcode = endcode + "    " +
            "for (int _i = 0; _i < " + deletede2s + ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "(" + deletede2s + "[_i]); }\n"; 
        
        if (c2 != ModelElement.ONE) 
        { String e2del = deletede2s + 
                         ".insert(" + deletede2s + ".end(), " + 
                                  ex + "->get" + role2 + "()->begin(), " +  
                                  ex + "->get" + role2 + "()->end());\n" + 
                         "    " + ex + "->setEmpty" + role2 + "();\n";
          midcode = midcode + e2del;
        } // ex->setEmptyrole2() is not necessary logically. 
        else 
        { String e2del = 
            "  if (" + ex + "->get" + role2 + "() != 0)\n" + 
            "  { " + deletede2s + ".push_back(" + ex + "->get" + role2 + "()); }\n" + 
            "  " + ex + "->set" + role2 + "(0);\n";  // OK since ex /: es at end
          midcode = midcode + e2del;
        } 
      }
      else if (e1.getName().equals(ename) && role1 != null && role1.length() > 0)
      { String e2name = e2.getName();
        String e2s = e2name.toLowerCase() + "_s";

        String abs2 = "";
        if (e2.isAbstract())
        { abs2 = "Abstract"; } 

        if (c1 == ModelElement.ONE) // kill all attached e2x's 
        { String removede2s = "_2removed" + role1 + e2name; 
          precode = precode + "    vector<" + e2name + "*> " + removede2s + ";\n"; 
          endcode = endcode + "    " +
            "for (int _i = 0; _i < " + removede2s + ".size(); _i++)\n" + 
            "    { kill" + abs2 + e2name + "(" + removede2s + "[_i]); }\n"; 
        } 
        midcode = midcode + ast.getDualKillCodeCPP(ex,e2s);
        if (c2 != ModelElement.ONE)  // Not logically needed
        { midcode = midcode + 
                    "\n    " + ex + "->setEmpty" + role2 + "();\n";
        }           
      } 
    }

    for (int k = 0; k < attributes.size(); k++)
    { Attribute att = (Attribute) attributes.get(k);
      if (att.isUnique())  // remove att from eattindex
      { String attnme = att.getName();
        String removeindex =  "    " + ename.toLowerCase() + attnme +
                              "index.erase(" + ex + "->get" + attnme + "());\n";
        midcode = midcode + removeindex;
      }
    }

    if (sup != null) 
    { endcode = endcode + "  kill" + sup.getName() + "(" + ex + ");\n"; }  
    for (int i = 0; i < interfaces.size(); i++)
    { Entity intf = (Entity) interfaces.get(i); 
      endcode = endcode + "  kill" + intf.getName() + "(" + ex + ");\n";
    }  

    endcode = endcode + killActionsCPP(ex, ename, cons, entities, types); 
    
    if (isActualLeaf())  
    { res = res + precode + midcode + endcode + "  // delete " + ex + ";\n" + 
          "  }\n\n";
    } 
    else  
    { res = res + precode + midcode + endcode + "\n" + 
          "  }\n\n";
    } 


    if (isAbstract())
    { Vector leafs = getActualLeafSubclasses(); 
      res = res + "  void killAbstract" + ename + 
                  "(" + ename + "* " + ex + ")\n";
      res = res + "  {\n"; 
      for (int ll = 0; ll < leafs.size(); ll++)
      { Entity lc = (Entity) leafs.get(ll);
        String lcname = lc.getName();  
        String lcs = lcname.toLowerCase() + "_s"; 
        res = res + "    if (find(" + lcs + "->begin(), " + lcs + "->end(), " + ex + ") != " + lcs + "->end())\n" + 
                    "    { kill" + lcname + "((" + lcname + "*) " + ex + "); }\n"; 
      } 
      res = res + "  }\n\n"; 
      res = res + "  void killAbstract" + ename + "(vector<" + ename + "*>* _l)\n";
      res = res + "  { for (int _i = 0; _i < _l->size(); _i++)\n" + 
                  "    { " + ename + "* _e = (*_l)[_i];\n" + 
                  "      killAbstract" + ename + "(_e);\n" + 
                  "    }\n" + 
                  "  }\n\n" + 
                  "  void killAbstract" + ename + "(set<" + ename + "*>* _l)\n" + 
                  "  { for (set<" + ename + "*>::iterator _i = _l->begin(); _i != _l->end(); ++_i)\n" + 
                  "    { " + ename + "* _e = *_i;\n" + 
                  "      killAbstract" + ename + "(_e);\n" + 
                  "    }\n" + 
                  "  }\n\n"; 
    } 

    return res;
  }

  private String killAllOp(String ename, String ex)
  { String res = "  public void killAll" + ename + "(List " + ex + ")\n"; 
    res = res + "  { for (int _i = 0; _i < " + ex + ".size(); _i++)\n" + 
          "    { kill" + ename + "((" + ename + ") " + ex + ".get(_i)); }\n" + 
          "  }\n\n"; 
    return res; 
  }  

  private String killAllOpJava6(String ename, String ex)
  { String res = "  public void killAll" + ename + "(Collection " + ex + ")\n"; 
    res = res + "  { for (Object _o : " + ex + ")\n" + 
          "    { kill" + ename + "((" + ename + ") _o); }\n" + 
          "  }\n\n"; 
    return res; 
  }  

  private String killAllOpJava7(String ename, String ex)
  { String res = "  public void killAll" + ename + "(Collection<" + ename + "> " + ex + ")\n"; 
    res = res + "  { for (Object _o : " + ex + ")\n" + 
          "    { kill" + ename + "((" + ename + ") _o); }\n" + 
          "  }\n\n"; 
    return res; 
  }  

  private String killAllOpCSharp(String ename, String ex)
  { String res = "  public void killAll" + ename + "(ArrayList " + ex + ")\n"; 
    res = res + "  { for (int _i = 0; _i < " + ex + ".Count; _i++)\n" + 
          "    { kill" + ename + "((" + ename + ") " + ex + "[_i]); }\n" + 
          "  }\n\n"; 
    return res; 
  }  

  private String killAllOpCPP(String ename, String ex)
  { String res = "  void killAll" + ename + "(vector<" + ename + "*>* " + ex + ")\n"; 
    res = res + "  { for (int _i = 0; _i < " + ex + "->size(); _i++)\n" + 
          "    { kill" + ename + "((*" + ex + ")[_i]); }\n" + 
          "  }\n\n" + 
          "  void killAll" + ename + "(set<" + ename + "*>* " + ex + ")\n" + 
          "  { for (set<" + ename + "*>::iterator _i = " + ex + "->begin(); _i != " + ex + "->end(); ++_i)\n" + 
          "    { kill" + ename + "(*_i); }\n" + 
          "  }\n\n"; 
    return res; 
  }  


  private String killActions(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("kill" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchKill(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateForm(env,false); }
      }
    }
    return res;
  }

  private String killActionsJava6(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("kill" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchKill(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormJava6(env,false); }
      }
    }
    return res;
  }

  private String killActionsJava7(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("kill" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchKill(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormJava7(env,false); }
      }
    }
    return res;
  }

  private String killActionsCSharp(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("kill" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchKill(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormCSharp(env,false); }
      }
    }
    return res;
  }

  private String killActionsCPP(String ex, 
    String ename, Vector cons, 
    Vector entities, Vector types)
  { String res = "";
    java.util.Map env = new java.util.HashMap();
    Attribute entx = new Attribute(ex,new Type(this),
                                    ModelElement.INTERNAL); 
    Vector v1 = new Vector();
    v1.add(entx);
    BehaviouralFeature ev =
      new BehaviouralFeature("kill" + ename,v1,false,null);

    for (int i = 0; i < cons.size(); i++)
    { Constraint cc = (Constraint) cons.get(i);
      Constraint cnew = cc.matchKill(ex,ename,ev);
      if (cnew != null)
      { Vector contexts = new Vector(); 
        contexts.add(this); 
        boolean typed = cnew.typeCheck(types,entities,contexts);
        if (typed)
        { res = res + "\n    " + cnew.updateFormCPP(env,false); }
      }
    }
    return res;
  }

  // Uses the unsafe (no argument) create op of an entity
  private String createAllOp(String ename, String ex)
  { String exx = ex + "_x"; 
    String res = "  public void createAll" + ename + "(List " + ex + ")\n"; 
    res = res + 
          "  { for (int i = 0; i < " + ex + ".size(); i++)\n" + 
          "    { " + ename + " " + exx + " = (" + ename + ") " + ex + ".get(i);\n" + 
          "      if (" + exx + " == null) { " + exx + " = new " + ename + "(); }\n" + 
          "      " + ex + ".set(i," + exx + ");\n" +
          "      add" + ename + "(" + exx + ");\n" +
          "    }\n" +   
          "  }\n\n"; 
    return res; 
  }  // and addename for each

  private String createAllOpJava6(String ename, String ex)
  { String exx = ex + "_x"; 
    String res = "  public void createAll" + ename + "(ArrayList " + ex + ")\n"; 
    res = res + 
          "  { for (int i = 0; i < " + ex + ".size(); i++)\n" + 
          "    { " + ename + " " + exx + " = new " + ename + "();\n" + 
          "      " + ex + ".set(i," + exx + ");\n" +
          "      add" + ename + "(" + exx + ");\n" +
          "    }\n" +   
          "  }\n\n"; 
    return res; 
  }  // and addename for each

  private String createAllOpJava7(String ename, String ex)
  { String exx = ex + "_x"; 
    String res = "  public void createAll" + ename + "(ArrayList<" + ename + "> " + ex + ")\n"; 
    res = res + 
          "  { for (int i = 0; i < " + ex + ".size(); i++)\n" + 
          "    { " + ename + " " + exx + " = new " + ename + "();\n" + 
          "      " + ex + ".set(i," + exx + ");\n" +
          "      add" + ename + "(" + exx + ");\n" +
          "    }\n" +   
          "  }\n\n"; 
    return res; 
  }  // and addename for each

  private String createAllOpCSharp(String ename, String ex)
  { String exx = ex + "_x"; 
    String res = "  public void createAll" + ename + "(ArrayList " + ex + ")\n"; 
    res = res + 
          "  { for (int i = 0; i < " + ex + ".Count; i++)\n" + 
          "    { " + ename + " " + exx + " = new " + ename + "();\n" + 
          "      " + ex + "[i] = " + exx + ";\n" +
          "      add" + ename + "(" + exx + ");\n" +
          "    }\n" +   
          "  }\n\n"; 
    return res; 
  }  // and addename for each

  private String createAllOpCPP(String ename, String ex)
  { String exx = ex + "_x"; 
    String res = "  void createAll" + ename + "(vector<" + ename + "*>* " + ex + ")\n"; 
    res = res + 
          "  { for (int i = 0; i < " + ex + "->size(); i++)\n" + 
          "    { " + ename + "* " + exx + " = new " + ename + "();\n" + 
          "      (*" + ex + ")[i] = " + exx + ";\n" +
          "      add" + ename + "(" + exx + ");\n" +
          "    }\n" +   
          "  }\n\n";
   /* res = res + "  void createAll" + ename + "(set<" + ename + "*>* " + ex + ")\n"; 
   res = res + 
          "  { set<" + ename + "*>::iterator _pos; \n" + 
          "  for (_pos = " + ex + "->begin(); _pos != " + ex + "->end(); ++_pos)\n" + 
          "    { *_pos = new " + ename + "();\n" + 
          "      add" + ename + "(*_pos);\n" +
          "    }\n" +   
          "  }\n\n"; */  
    return res; 
  }  // and addename for each


  private String cardinalityCheckCode(String es)
  { if (cardinality == null || 
        cardinality.equals("") ||
        cardinality.equals("*"))
    { return ""; }
    int num;
    try
    { num = Integer.parseInt(cardinality); }
    catch (Exception e)
    { String nums = cardinality.substring(3); // 0..nums
      try
      { num = Integer.parseInt(nums); }
      catch (Exception e2)
      { System.err.println("Invalid cardinality format: " +
                           cardinality); 
        return "";
      }
    }
    return "    if (" + es + ".size() >= " + num +
           ") { return null; }\n";
  } 

  private String cardinalityCheckCodeCSharp(String es)
  { if (cardinality == null || 
        cardinality.equals("") ||
        cardinality.equals("*"))
    { return ""; }
    int num;
    try
    { num = Integer.parseInt(cardinality); }
    catch (Exception e)
    { String nums = cardinality.substring(3); // 0..nums
      try
      { num = Integer.parseInt(nums); }
      catch (Exception e2)
      { System.err.println("Invalid cardinality format: " +
                           cardinality); 
        return "";
      }
    }
    return "    if (" + es + ".Count >= " + num + ") { return null; }\n";
  } // and for C++? 

  private String cardinalityCheckCodeCPP(String es)
  { if (cardinality == null || 
        cardinality.equals("") ||
        cardinality.equals("*"))
    { return ""; }
    int num;
    try
    { num = Integer.parseInt(cardinality); }
    catch (Exception e)
    { String nums = cardinality.substring(3); // 0..nums
      try
      { num = Integer.parseInt(nums); }
      catch (Exception e2)
      { System.err.println("Invalid cardinality format: " +
                           cardinality); 
        return "";
      }
    }
    return "    if (" + es + "->size() >= " + num + ") { return null; }\n";
  } // and for C++? 

  public String toXml()
  { String res = "  <UML:Class name=\"" + getName() +
                 "\">\n";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String feat = att.toXml();
      res = res + feat;
    }
    for (int j = 0; j < invariants.size(); j++)
    { Constraint c = (Constraint) invariants.get(j);
      res = res + c.toXml();
    }
    return res + "  </UML:Class>\n";
  }   // and ops and associations

  public String getResultPage()
  { String res =
          "import java.sql.*;\n\n" + 
          "public class " + getName() + "ResultPage extends BasePage\n" +
          "{ private HtmlTable table = new HtmlTable(); \n" +
          "  private HtmlTableRow header = new HtmlTableRow();\n\n" +
          "  public " + getName() + "ResultPage()\n" +
          "  { table.setAttribute(\"border\",\"2\");\n";
    int n = attributes.size();
    for (int i = 0; i < n; i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attName = att.getName();
      res = res + "    header.addCell(new HtmlTableData(\"" + attName +
            "\"));\n";
    }
    res = res + "    table.addRow(header);\n"; 
    res = res + "    body.add(table);\n  }\n\n" +
          "  public void addRow(ResultSet resultSet)\n" +
          "  { HtmlTableRow row = new HtmlTableRow();\n" +
          "    try {\n";
    for (int i = 0; i < n; i++)
    { Attribute att = (Attribute) attributes.get(i);
      String getAtt = att.jdbcExtractOp("resultSet");
      res = res + "      row.addCell(new HtmlTableData(\"\" + " + getAtt + "));\n";
    }
    res = res + "    } catch (Exception e) { e.printStackTrace(); }\n" +
                "    table.addRow(row);\n" +
                "  }\n" +
                "}\n"; 
    return res;
  }

  public String getTableHeader()
  { String res = "<tr>";
    int n = attributes.size();
    for (int i = 0; i < n; i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attName = att.getName();
      res = res + "<th>" + attName + "</th> ";
    }
    res = res + "</tr>";
    return res; 
  }

  public String getTableRow() 
  { String res = "<tr>";
    String obj = getName().toLowerCase() + "VO"; 
    int n = attributes.size();
    for (int i = 0; i < n; i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attName = att.getName();
      res = res + "<td><%= " + obj + ".get" + attName + "() %></td> ";
    }
    return res + "</tr>";
  }

  public Vector getInvariantCheckTests(Vector params)
  { // only include invariants which have all features in params
    Vector parnames = ModelElement.getNames(params); 
    Vector res = new Vector(); 
    if (invariants.size() == 0) { return res; } 
    Vector newinvs = new Vector();
    Vector oldinvs = new Vector(); 
 
    // Vector oldinvs = (Vector) ((Vector) invariants).clone(); 
    for (int i = 0; i < invariants.size(); i++)
    { Constraint c = (Constraint) invariants.get(i); 
      Vector cfeats = c.allFeaturesUsedIn(); 
      if (parnames.containsAll(cfeats))
      { oldinvs.add((Constraint) c.clone()); } 
    }
     
    java.util.Map env = new java.util.HashMap(); 
    env.put(getName(),"this"); 

    for (int i = 0; i < params.size(); i++)
    { Attribute att = (Attribute) params.get(i);
      String attname = att.getName();  
      Type t = att.getType(); 
      String tname = t.getName();
      Expression newE; 
      if (tname.equals("int"))
      { newE = new BasicExpression("i" + attname);
        newE.setUmlKind(Expression.VARIABLE); 
        newE.setType(t); 
        newinvs = Constraint.substituteEqAll(attname,newE,oldinvs); 
        oldinvs = (Vector) ((Vector) newinvs).clone(); 
      }
      else if (tname.equals("double"))
      { newE = new BasicExpression("d" + attname);
        newE.setUmlKind(Expression.VARIABLE); 
        newE.setType(t);
        newinvs = Constraint.substituteEqAll(attname,newE,oldinvs); 
        oldinvs = (Vector) ((Vector) newinvs).clone(); 
      }    
    }
    
    for (int j = 0; j < oldinvs.size(); j++) 
    { Constraint con = (Constraint) oldinvs.get(j); 
      String contest = con.queryForm(env,true); 
      res.add(contest); 
    } 
    return res; 
  } 

  public boolean checkAttributeRedefinitions()
  { if (superclass == null && 
        interfaces.size() == 0) 
    { return true; } // ok
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      if (superclass.hasInheritedAttribute(att.getName()))
      { System.err.println("Error: attribute " + att +
          " defined in " + this + " and an " +
          " ancestor class");
        return false;   
      }
      for (int j = 0; j < interfaces.size(); j++) 
      { Entity intf = (Entity) interfaces.get(j); 
        if (intf.hasInheritedAttribute(att.getName()))
        { System.err.println("Error: attribute " + att +
            " defined in " + this + " and an " +
            " ancestor interface");
          return false;
        }   
      }
    }
    return true;
  }

  public boolean selfImplementing()
  { if (isInterface()) // others are checked already
    { Vector v = getAllInterfaces(new Vector());
      if (v.contains(this)) { return true; }
    }
    return false;
  }

  private Vector getAllInterfaces(Vector path)
  { for (int i = 0; i < interfaces.size(); i++)
    { Entity intf = (Entity) interfaces.get(i);
      if (path.contains(intf)) { }
      else 
      { path.add(intf); 
        intf.getAllInterfaces(path); 
      }
    }
    return path;
  } 

  public Vector hasDuplicateInheritance()
  { // returns entities that it inherits directly & also indirectly
    if (superclass != null)
    { Vector sinterfaces = superclass.getAllInterfaces(new Vector()); 
      sinterfaces.retainAll(interfaces); 
      if (sinterfaces.size() > 0)
      { return sinterfaces; } 
    }
    for (int i = 0; i < interfaces.size(); i++) 
    { Entity intf = (Entity) interfaces.get(i); 
      Vector iinterfaces = intf.getAllInterfaces(new Vector()); 
      iinterfaces.retainAll(interfaces); 
      if (iinterfaces.size() > 0)
      { return iinterfaces; } 
    }
    return new Vector(); 
  }

  public static void introduceSuperclass(Entity[] ents,UCDArea ucdArea)
  { // look for common features in the ents
    Vector commonatts = (Vector) ents[0].getAttributes().clone(); 
    Vector commonops = (Vector) ents[0].getOperations().clone(); 
    Vector commonroles = (Vector) ents[0].getAssociations().clone(); 
    String name = ents[0].getName(); 
    // common constraints can go in new class if only involve its features
    Vector removed = new Vector(); 

    for (int i = 1; i < ents.length; i++)
    { Entity e = ents[i];
      name = name + "or" + e.getName(); 
      for (int j = 0; j < commonatts.size(); j++) 
      { Attribute att = (Attribute) commonatts.get(j); 
        Attribute eatt = e.getAttribute(att.getName()); 
        if (eatt == null) 
        { removed.add(att); }
        else // check types, etc are same
        { Attribute newatt = att.mergeAttribute(eatt);
          if (newatt == null) 
          { removed.add(att); }
          else 
          { commonatts.set(j,newatt); }  
        }
      }
      commonatts.removeAll(removed); 
      removed = new Vector(); 
    }

    Vector newstereo = new Vector(); 
    newstereo.add("abstract"); 
    Entity newe = ucdArea.reconstructEntity(name,10,10,"","*",newstereo); 
    newe.setAttributes(commonatts); // remove all commonatts from the subclasses
    ucdArea.addInheritances(newe,ents); 

    for (int i = 1; i < ents.length; i++)
    { Entity e = ents[i];
      for (int j = 0; j < commonroles.size(); j++) 
      { Association ast = (Association) commonroles.get(j); 
        Association east = e.getRole(ast.getRole2()); 
        if (east == null) 
        { removed.add(ast); }
        else // check types, etc are same
        { Association newast = ast.mergeAssociation(newe,east);
          if (newast == null) 
          { removed.add(ast); }
          else 
          { commonroles.set(j,newast); }  
        }
      }
      commonroles.removeAll(removed); 
      removed = new Vector(); 
    }

    for (int i = 1; i < ents.length; i++)
    { Entity e = ents[i];
      for (int j = 0; j < commonops.size(); j++) 
      { BehaviouralFeature op = (BehaviouralFeature) commonops.get(j); 
        BehaviouralFeature eop = e.getOperation(op.getName()); 
        if (eop == null) 
        { removed.add(op); }
        else // check types, etc are same
        { BehaviouralFeature newop = op.mergeOperation(newe,eop);
          if (newop == null) 
          { removed.add(op); }
          else 
          { commonops.set(j,newop); }  
        }
      } 
      commonops.removeAll(removed); 
      removed = new Vector(); 
    }

    newe.setAssociations(commonroles); 
    newe.setOperations(commonops); 
    newe.setAbstract(true); 
    for (int i = 0; i < ents.length; i++)
    { Entity e = ents[i];
      e.removeAtts(commonatts); 
    }  // and common roles?
    ucdArea.addAssociations(commonroles); 
  }

  // removeQualifiedAssociation is similar

  public void removeAssociationClass(UCDArea ucdArea)
  { if (linkedAssociation != null) 
    { Entity e1 = linkedAssociation.getEntity1(); 
      Entity e2 = linkedAssociation.getEntity2(); 
      String role2 = linkedAssociation.getRole2(); 
      // create new associations to these
      String e1name = e1.getName(); 
      String e2name = e2.getName(); 
      String e1role = e1name.toLowerCase(); 
      String e2role = e2name.toLowerCase(); 
      String ename = getName(); 
      String er = ename.toLowerCase() + "r";  
      Association toe1 = new Association(this,e1,MANY,ONE,er,e1role); 
      Association toe2 = new Association(this,e2,MANY,ONE,"",e2role); 
      ucdArea.removeAssociationClass(linkedAssociation); // to normal assoc
      associations.add(toe1); 
      associations.add(toe2);
      e1.addAssociation(new Association(e1,this,ONE,MANY,e1role,er)); 
      Vector newassocs = new Vector(); 
      newassocs.add(toe1);
      newassocs.add(toe2); 
      ucdArea.addAssociations(newassocs);
      // create new invariant: er.e2role = role2 of e1
      BasicExpression erbe = new BasicExpression(er); 
      BasicExpression e2rolebe = new BasicExpression(e2role); 
      e2rolebe.setObjectRef(erbe); 
      BasicExpression role2be = new BasicExpression(role2); 
      Expression eq = new BinaryExpression("=",e2rolebe,role2be); 
      Invariant inv = new SafetyInvariant(new BasicExpression("true"),eq); 
      ucdArea.addInvariant(inv,e1); 
      linkedAssociation = null; 
    } 
  }  

  public int getSmvCardinality()
  { int res = -1; 
    if (cardinality == null || cardinality.equals("*"))
    { return res; } 
    try { res = Integer.parseInt(cardinality); }
    catch (Exception e) 
    { return -1; }
    return res; 
  }

  public Vector smvEventList()
  { Vector list = new Vector(); 
    if (hasStereotype("source")) { return list; } 

    String nme = getName(); 
    list.add("create" + nme); 
    list.add("kill" + nme); 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.getKind() == ModelElement.SEN)
      { String anme = att.getName(); 
        Type t = att.getType(); 
        Vector vals = t.getSmvValues(); 
        if (vals == null || vals.size() == 0) { } // not valid in SMV
        else  
        { for (int j = 0; j < vals.size(); j++) 
          { String val = (String) vals.get(j); 
            list.add(anme + val); 
          } 
        }
      } 
    } // and associations
    for (int k = 0; k < associations.size(); k++) 
    { Association ast = (Association) associations.get(k); 
      Entity ent2 = ast.getEntity2(); 
      int c2 = ent2.getSmvCardinality(); 
      if (c2 > 0)
      { String role2 = ast.getRole2(); 
        list.add("add" + role2); 
        list.add("rem" + role2); 
      } 
    } 
    return list; 
  } 

  public Vector getEvents()
  { Vector list = new Vector(); 
    String nme = getName(); 
    // list.add(new Event("create" + nme)); 
    // list.add(new Event("kill" + nme)); 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      // if (att.getKind() == ModelElement.SEN)
      if (att.isFrozen()) { } 
      else 
      { String anme = att.getName(); 
        // Type t = att.getType(); 
        // Vector vals = t.getSmvValues(); 
        // if (vals == null || vals.size() == 0) { } // not valid in SMV
        // else  
        // { for (int j = 0; j < vals.size(); j++) 
        //   { String val = (String) vals.get(j); 
        //     list.add(anme + val); 
        //   } 
        // }
        list.add(new Event("set" + anme)); 
      } 
    } // and associations
    for (int k = 0; k < associations.size(); k++) 
    { Association ast = (Association) associations.get(k);
      if (ast.isFrozen()) { } 
      else 
      { String role2 = ast.getRole2(); 
        list.add(new Event("set" + role2)); 
        if (ast.getCard2() != ModelElement.ONE)
        { list.add(new Event("add" + role2)); 
          if (ast.isAddOnly()) { } 
          else
          { list.add(new Event("remove" + role2)); } 
        } 
      } 
    }
    for (int q = 0; q < operations.size(); q++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(q); 
      if (bf.isUpdate())
      { list.add(new Event(bf.getName())); } 
    }  
    // if (isActive())
    // { list.add(new Event("run")); } 
    return list; 
  }

  public Vector getDefinedEvents()
  { Vector list = getEvents(); 
    if (superclass != null)
    { list.addAll(superclass.getDefinedEvents()); } 
    return list; 
  } 

  public Vector getEventNames()
  { Vector list = new Vector(); 
    String nme = getName(); 
    // list.add("create" + nme); 
    // list.add("kill" + nme); 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isFrozen()) { } 
      else 
      // if (att.getKind() == ModelElement.SEN)
      { String anme = att.getName(); 
        // Type t = att.getType();   Only add if not private or derived
        // Vector vals = t.getSmvValues(); 
        // if (vals == null || vals.size() == 0) { } // not valid in SMV
        // else  
        // { for (int j = 0; j < vals.size(); j++) 
        //   { String val = (String) vals.get(j); 
        //     list.add(anme + val); 
        //   } 
        // }
        list.add("set" + anme); 
      } 
    } // and associations
    for (int k = 0; k < associations.size(); k++) 
    { Association ast = (Association) associations.get(k); 
      if (ast.isFrozen()) { } 
      else 
      { String role2 = ast.getRole2(); 
        list.add("set" + role2); 
        if (ast.getCard2() != ModelElement.ONE)
        { list.add("add" + role2); 
          if (ast.isAddOnly()) { } 
          else
          { list.add("remove" + role2); } 
        } 
      } 
    }
    for (int q = 0; q < operations.size(); q++) 
    { BehaviouralFeature bf = (BehaviouralFeature) operations.get(q); 
      if (bf.isUpdate())
      { list.add(bf.getName()); } 
    }  
    // if (isActive())
    // { list.add("run"); } 

    return list; 
  } 

  public Vector getDefinedEventNames()
  { Vector list = getEventNames(); 
    if (superclass != null)
    { list.addAll(superclass.getDefinedEventNames()); } 

    return list; 
  } 

  public Vector getSupplierEvents()
  { Vector res = new Vector(); 
    for (int k = 0; k < associations.size(); k++) 
    { Association ast = (Association) associations.get(k); 
      Entity e2 = ast.getEntity2(); 
      res.addAll(e2.getEvents());  // remove duplicates
    }
    return res; 
  } 

  public Vector getSuppliers()
  { Vector res = new Vector(); 
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      Entity ent2 = ast.getEntity2(); 
      if (res.contains(ent2)) { } 
      else 
      { res.add(ent2); } 
    } 
    return res; 
  } 

  public String getValueObject()
  { String res = "package beans;\n";
    String nme = getName();  
    res = res + "public class " + nme + "VO\n" + 
          "{ \n"; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attnme = att.getName(); 
      String tname = att.getType().getJava(); 
      if (tname.equals("boolean"))
      { tname = "String"; } 
      res = res + " private " + tname + " " + attnme + ";\n"; 
    } 
    res = res + "\n" +
          "  public " + nme + "VO(";
    boolean previous = false;
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String tname = att.getType().getJava(); 
      if (tname.equals("boolean"))
      { tname = "String"; } 

      String par = tname + " " + att.getName() + "x";
      if (previous)
      { res = res + "," + par; }
      else        
      { res = res + par;
        previous = true;
      }
    }
    res = res + ")\n  { "; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attnme = att.getName(); 
      res = res + "   " + attnme + " = " + attnme + "x;\n"; 
    }
    res = res + "  }\n\n"; 
 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attnme = att.getName(); 
      String tname = att.getType().getJava(); 
      if (tname.equals("boolean"))
      { tname = "String"; } 

      res = res + "  public " + tname + " get" + attnme + "()\n  { " + 
            "return " + attnme + "; }\n\n"; 
    } 

    return res + "}\n\n"; 
  } 
 
  public String generateBean(Vector useCases, Vector cons, Vector entities,
                             Vector types)
  { String ename = getName(); 
    String res = "package beans;\n\n" + 
      "import java.util.*;\n" + 
      "import java.sql.*;\n\n" + 
      "public class " + ename + "Bean\n{ Dbi dbi = new Dbi();\n"; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attnme = att.getName(); 
      String tname = att.getType().getName(); 
      res = res + " private String " + attnme + " = \"\";\n";
      if (tname.equals("int"))
      { res = res + " private int i" + attnme + " = 0;\n"; } 
      else if (tname.equals("double"))
      { res = res + " private double d" + attnme + " = 0;\n"; } 
      // booleans are treated as strings. 
    } 
    res = res + "  private Vector errors = new Vector();\n\n" +
          "  public " + ename + "Bean() {}\n\n"; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attnme = att.getName(); 
      res = res + "  public void set" + attnme + "(String " + attnme + "x)\n  { " + 
            attnme + " = " + attnme + "x; }\n\n"; 
    } 

    res = res + "  public void resetData()\n  { "; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      String attname = att.getName(); 
      res = res + attname + " = \"\";\n  "; 
    } 
    res = res + "}\n\n";     

    for (int j = 0; j < useCases.size(); j++)
    { if (!(useCases.get(j) instanceof OperationDescription)) { continue; } 

      OperationDescription od = (OperationDescription) useCases.get(j); 
      if (this != od.getEntity()) { continue; } 

      Vector pars = od.getParameters(); 
      String odname = od.getODName(); 
      String action = od.getStereotype(0); 
      // build op that checks if parameters are correct, and does data conversions:
      res = res + "  public boolean is" + odname + "error()\n" + 
            "  { errors.clear(); \n"; 
      for (int k = 0; k < pars.size(); k++) 
      { Attribute att = (Attribute) pars.get(k); 
        String check = att.getBeanCheckCode(); 
        res = res + check; 
      } 
      if (action.equals("create") || action.equals("edit") || action.equals("set"))
      { Vector tests = getInvariantCheckTests(pars); 
        for (int p = 0; p < tests.size(); p++)
        { String test = (String) tests.get(p); 
          res = res + 
                "    if (" + test + ") { }\n" + 
                "    else { errors.add(\"Constraint: " + test + " failed\"); }\n";
        }
      }
      res = res + "  return errors.size() > 0; }\n\n";
    }

    res = res + "  public String errors() { return errors.toString(); }\n\n"; 

    for (int j = 0; j < useCases.size(); j++)
    { OperationDescription od = (OperationDescription) useCases.get(j); 
      // and is responsibility of this bean
      if (this != od.getEntity()) { continue; } 

      Vector pars = od.getParameters(); 
      String odname = od.getODName(); 
      String action = od.getStereotype(0); 
      String dbiop = od.getDbiOp(); 
      Vector correc = new Vector(); 

      if (action.equals("create") || action.equals("delete") || 
          action.equals("add") || action.equals("remove") ||
          action.equals("edit") || action.equals("set"))
      { res = res + "  public void " + odname + "()\n" +  "  { "; 
        res = res + dbiop + "\n    ";  
        if (action.equals("set"))
        { Attribute att = (Attribute) pars.get(0); 
          Vector allinvs = new Vector();
          allinvs.addAll(invariants); 
          allinvs.addAll(cons); 

          correc = att.sqlSetOperations(this,allinvs,entities,types); 
          res = res + odname + "(" + att.getBeanForm() + 
                               ", i" + ename.toLowerCase() + "Id);\n  ";
        }
        res = res + "resetData(); }\n\n";
        if (correc.size() > 0)
        { res = res + correc.get(0) + "\n\n"; 
          correc.remove(0); 
          od.addDbiMaintainOps(correc); 
        } 
      }   // for set, correcting code goes here as well. 
      else 
      { Entity ent2 = this; 

        if (action.equals("get"))
        { String role = od.getStereotype(1); 
          Association ast = getRole(role); 
          if (ast != null)
          { ent2 = ast.getEntity2(); }
        }

        res = res + "  public Iterator " + odname + "()\n" +  "  { "; 
        res = res + "ResultSet rs = " + dbiop + "\n" +
              "   List rs_list = new ArrayList();\n" +  
              "   try \n" + 
              "   { while (rs.next())\n" + 
              "     { " + ent2.jspExtractCode(ent2.getAttributes()) + 
              "     }\n" + 
              "   } catch (Exception e) { }\n" + 
              "   resetData();\n" + 
              "   return rs_list.iterator();\n  }\n"; 
      }  // for getrole it is the TARGET entities fields
    }
    return res + "}\n"; 
  }

  private String jspExtractCode(Vector pars)
  { String ename = getName(); 
    String res = "rs_list.add(new " + ename + "VO("; 
    for (int i = 0; i < pars.size(); i++)
    { Attribute att = (Attribute) pars.get(i); 
      String attname = att.getName(); 
      String jdbcop = att.jdbcExtractOp("rs");  // rs.getInt() etc
      if (i < pars.size() - 1)
      { res = res + jdbcop + ","; } 
      else 
      { res = res + jdbcop; } 
    } 
    return res + "));"; 
  }

  public String ejbBean()
  { String beanName = getName() + "Bean"; 
    String res = "package beans;\n" + 
      "import java.util.*;\n" + 
      "import javax.ejb.*;\n" + 
      "import javax.naming.*;\n\n" + 
      "public abstract class " + beanName + " extends EntityBean\n" + 
      "{ private EntityContext context;\n"; 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      res = res + att.ejbBeanGet() + "\n" + att.ejbBeanSet() + "\n"; 
    } 
    // also set and get for associations, then ejbCreate, etc
    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      res = res + ast.ejbBeanGet() + "\n" + ast.ejbBeanSet() + "\n"; 
    } 
    // include add/remove for multiple ones if these are use cases
    res = res + ejbBeanCreate(); 
    res = res + ejbMethods();  
    return res + "\n\n"; 
  } 

  public String ejbBeanCreate()
  { String res = 
      "  public String ejbCreate(";
    String body = "";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attnme = att.getName();
      String tname = att.getType().getJava();
      res = res + tname + " " + attnme + "x";
      if (i < attributes.size() - 1)
      { res = res + ","; }
      String fl = attnme.substring(0,1);
      String rem = 
        attnme.substring(1,attnme.length());

      body = body + "  set" +
             fl.toUpperCase() + rem + 
             "(" + attnme + "x);\n";
    }
    res = res + ")\n" +
          "  throws CreateException\n" +
          "  { " + body + "\n  " +
          "    return null; }\n\n";
    return res;
  }

  public String ejbMethods()
  { String res = 
      "  public void ejbRemove() { }\n" +
      "  public void setEntityContext(EntityContext ctx)\n" +
      "  { context = ctx; }\n" +
      "  public void unsetEntityContext()\n" +
      "  { context = null; }\n" +
      "  public void ejbLoad() { }\n" +
      "  public void ejbStore() { }\n" +
      "  public void ejbActivate() { }\n" +
      "  public void ejbPassivate() { }\n";
    res = res +    
      "  public void ejbPostCreate(";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attnme = att.getName();
      String tname = att.getType().getJava();
      res = res + tname + " " + attnme + "x";
      if (i < attributes.size() - 1)
      { res = res + ","; }
    }
    res = res + ") { } \n}";
    return res;
  }

  
  public String genEJBLocalRemote(boolean local)
  { String locrem = "";
    if (local)
    { locrem = "Local"; }
    String res = "package beans;\n\n";
    res = res + "import java.util.*;\n" +
          "import javax.ejb.*;\n\n";
    String nme = getName();
    res = res + "public interface " + locrem + nme +
          " extends EJB" + locrem + "Object\n{";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      res = res + "  " + att.ejbBeanGet() + "\n" +
            att.ejbBeanSet() + "\n";
    }
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i);
      res = res + "  " + ast.ejbBeanGet() + "\n" +
            ast.ejbBeanSet() + "\n";
    }
    res = res + "}\n\n";
    return res;
  }

  public String genEJBHome(boolean local)
  { String locrem = "";
    if (local)
    { locrem = "Local"; }
    String ename = getName();
    String res = "package beans;\n\n";
    res = res + "import java.util.*;\n" +
      "import javax.ejb.*;\n\n" +
      "public interface " + locrem + ename + 
      "Home extends EJB" + locrem + "Home\n" +
      "{ public " + locrem + ename +
      " create(";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String attnme = att.getName();
      String tname = att.getType().getJava();
      res = res + tname + " " + attnme + "x";
      if (i < attributes.size() - 1)
      { res = res + ","; }
    }
    res = res + ")\n";
    res = res + "  throws CreateException;\n\n";

    res = res + "  public " + locrem + ename + 
          " findByPrimaryKey(String id)\n";     
    res = res + "  throws FinderException;\n}\n";
    // Look up the primary key type. + other getBy methods on the entity
    return res; 
  }

  public String getSessionBean(Vector ucs) // for entity
  { String res = "package beans;\n\n";
    res = res + "import java.util.*;\n" +
          "import javax.ejb.*;\n" +
          "import javax.naming.*;\n" +
          "import java.rmi.RemoteException;\n\n";

    String ename = getName();
    String inst = ename.toLowerCase();
    
    res = res +  "public class " + ename + 
          "SessionBean implements SessionBean\n" +
          "{ private Local" + ename + "Home " +
          inst + "Home;\n\n";
    // private String accountId;
   
    res = res + 
          "  public " + ename + "SessionBean() { }\n\n";
    res = res + ejbSessionCreate();
    res = res + ejbSessionRemove();
    for (int i = 0; i < ucs.size(); i++)
    { OperationDescription uc = (OperationDescription) ucs.get(i);
      if (uc.getStereotype(0).equals("add"))
      { String role = uc.getStereotype(1);
        res = res + ejbSessionAddRole(role);
      }
      if (uc.getStereotype(0).equals("remove"))
      { String role = uc.getStereotype(1);
        res = res + ejbSessionRemoveRole(role);
      }
    }
    return res + "}\n\n";
  }

  public String ejbSessionCreate()
  { String ename = getName();
    String inst = ename.toLowerCase();
    String res = 
      "  public String create" + ename +
      "(" + ename + "VO " + inst + "x)\n" +
      "  throws InvalidParameterException\n" +
      "  { Local" + ename + " " + inst + " = null;\n";
    // checks on data
    res = res + 
      "  try\n" +
      "  { nextId = getNextId(); \n" +
      "    " + inst + " = " +
      inst + "Home.create(nextId," +
      getDetailList(inst + "x") + ");\n" +
      "  }\n" +
      "  catch (Exception ex)\n" +
      "  { throw new EJBException(ex.getMessage()); }\n" +

      "  return " + inst + ".get" + ename + "Id();\n";
    return res + "  }\n\n"; 
  }

  public String getDetailList(String inst)
  { String res = "";
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i);
      String nme = att.getName();
      res = res + inst + ".get" + capitalise(nme) + "()";
      if (i < attributes.size() - 1)
      { res = res + ","; }
    }
    return res;
  }

  public String ejbSessionRemove()
  { String ename = getName();
    String inst = ename.toLowerCase();
    String res =
      "  public void remove" + ename +
      "(String " + inst + "Id)\n" +
      "  throws InvalidParameterException\n";
    res = res + "  { Local" + ename + " " + inst +
      " = null;\n";
    res = res +
      "    if (" + inst + "Id == null)\n" +
      "    { throw new InvalidParameterException(\"null Id\"); }\n";

    res = res +
      "    try\n" +
      "    { " + inst + " = " + inst + 
      "Home.findByPrimaryKey(" + inst + "Id);\n" +
      "      " + inst + ".remove();\n" +
      "    } catch (Exception ex)\n" +
      "    { throw new EJBException(ex.getMessage()); }\n";
    return res + "  }\n\n";
  }

  public String ejbSessionAddRole(String role)
  { String ename = getName();
    String inst = ename.toLowerCase();
    Association ast = getRole(role);
    Entity e2 = ast.getEntity2();
    String e2name = e2.getName();
    String e2inst = e2name.toLowerCase();
    String res =
      "  public void add" + ename + role +
      "(String " + inst + "Id, String " + 
      e2inst + "Id)\n" +
      "  throws InvalidParameterException\n";
    res = res +
      "  { Local" + ename + " " + inst + " = null;\n" +
      "    Local" + e2name + " " + e2inst + " = null;\n";

    res = res +
      "    if (" + inst + "Id == null)\n" +
      "    { throw new InvalidParameterException(\"null " + 
      inst + "Id\"); }\n" +
      "    else if (" + e2inst + "Id == null)\n" +
      "    { throw new InvalidParameterException(\"null " + 
      e2inst + "Id\"); }\n";

    res = res + 
      "    try \n" +
      "    { " + inst + " = " + inst + 
      "Home.findByPrimaryKey(" + inst + "Id); }\n" +
      "    catch (Exception ex) \n" +
      "    { throw new EJBException(ex.getMessage()); }\n" +

      "    try \n" +
      "    { " + e2inst + " = " + e2inst +
      "Home.findByPrimaryKey(" + e2inst + "Id);\n" +
      "      " + inst + ".add" + role + "(" +
      e2inst + ");\n" +
      "    } catch (Exception ex)\n" +
      "    { throw new EJBException(ex.getMessage()); }\n";
    return res + "  }\n\n";
  }

  public String ejbSessionRemoveRole(String role)
  { String ename = getName();
    String inst = ename.toLowerCase();
    Association ast = getRole(role);
    Entity e2 = ast.getEntity2();
    String e2name = e2.getName();
    String e2inst = e2name.toLowerCase();
    String res =
      "  public void remove" + ename + role +
      "(String " + inst + "Id, String " + 
      e2inst + "Id)\n" +
      "  throws InvalidParameterException\n";
    res = res +
      "  { Local" + ename + " " + inst + " = null;\n" +
      "    Local" + e2name + " " + e2inst + " = null;\n";

    res = res +
      "    if (" + inst + "Id == null)\n" +
      "    { throw new InvalidParameterException(\"null " + 
      inst + "Id\"); }\n" +
      "    else if (" + e2inst + "Id == null)\n" +
      "    { throw new InvalidParameterException(\"null " + 
      e2inst + "Id\"); }\n";

    res = res + 
      "    try \n" +
      "    { " + inst + " = " + inst + 
      "Home.findByPrimaryKey(" + inst + "Id); }\n" +
      "    catch (Exception ex) \n" +
      "    { throw new EJBException(ex.getMessage()); }\n" +

      "    try \n" +
      "    { " + e2inst + " = " + e2inst +
      "Home.findByPrimaryKey(" + e2inst + "Id);\n" +
      "      " + inst + ".remove" + role + "(" +
      e2inst + ");\n" +
      "    } catch (Exception ex)\n" +
      "    { throw new EJBException(ex.getMessage()); }\n";
    return res + "  }\n\n";
  }

  public void addStateInvariants(Attribute att, Vector states, Vector types,
                                 Vector entities)
  { BasicExpression attbe = new BasicExpression(att.getName()); 
    
    for (int i = 0; i < states.size(); i++) 
    { State st = (State) states.get(i); 
      Maplet prop = st.getProperties(); 
      { if (prop != null && prop.dest != null)
        { Vector invs = (Vector) prop.dest;
          BasicExpression stexp = new BasicExpression(st.label);  
          for (int j = 0; j < invs.size(); j++) 
          { try
            { Expression exp = (Expression) invs.get(j);
              SafetyInvariant si = 
                new SafetyInvariant(
                  new BinaryExpression("=",attbe,stexp),exp); 
              
              Constraint cc = new Constraint(si,new Vector()); 
              cc.setOwner(this);
              cc.setLocal(true);  
              Vector contexts = new Vector(); 
              contexts.add(this); 
        
              boolean ok = cc.typeCheck(types,entities,contexts); 
              if (ok)
              { invariants.add(cc); } // But must only use features from this
              else 
              { System.err.println("Invalid state invariant: " + cc); } 
            } catch(Exception e) 
              { System.err.println("Invalid invariants: " + invs); } 
          } 
        }
      }
    }
  }
               

  public Vector addTransitionConstraints(Vector trans, Vector types, 
                                         Vector entities)
  { Vector res = new Vector(); 
    // Global invariants, from the generations

    String attname = getName().toLowerCase() + "State";       
    Attribute att = 
      (Attribute) getFeature(attname);
    if (att == null) { return res; }  
    BasicExpression attbe = new BasicExpression(att); 

    for (int i = 0; i < trans.size(); i++) 
    { // src -->e[G]/act  trg
      // gives invariants   e & estate = src & G => AX(estate = trg) & act
      Transition t = (Transition) trans.get(i); 
      Event e = t.event; 
      State src = t.source; 
      State trg = t.target; 

      if (src == null || trg == null) 
      { System.err.println("Invalid transition: " + t); 
        continue; 
      } 

      Expression g = t.getGuard(); 
      if ("true".equals("" + g))
      { g = null; } 

      Vector gens = t.getGenerations(); 
      Vector pars = new Vector(); 

      BinaryExpression eq = 
        new BinaryExpression("=",attbe,new BasicExpression(src.label)); 
      // and the guard

      BehaviouralFeature ebe0, ebe;
      ebe0 = getOperation(e.label); 
      if (ebe0 == null)
      { pars = deduceParameters(e.label); 
        ebe = new BehaviouralFeature(e.label,pars,false,null);
      } 
      else 
      { ebe = ebe0;
        pars = ebe0.getParameters(); 
      } 

      // ebe.setIsEvent(); 
      BinaryExpression eq1 = 
        new BinaryExpression("=",attbe,new BasicExpression(trg.label));
      Constraint con = new Constraint(ebe,eq,g,eq1,new Vector()); 
      if (ebe0 != null)
      { BasicExpression atbe = new BasicExpression(attname); 
        atbe.setPrestate(true); 
        Expression eq2 = 
          new BinaryExpression("=",atbe,new BasicExpression(src.label)); 
      
        Expression ante = Expression.simplify("&",eq2,g,new Vector()); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        Expression newpost = new BinaryExpression("=>",ante,eq1);
        newpost.setBrackets(true); 
        newpost.typeCheck(types,entities,contexts,pars); 
        ebe0.addPost(newpost); 
      } // attributes in @pre form in ante
      else if (invariants.contains(con)) { } 
      else 
      { con.setOwner(this); 
        con.setLocal(true); 
        Vector contexts = new Vector(); 
        contexts.add(this); 
        
        con.typeCheck(types,entities,contexts); 
        invariants.add(con); 
      }
      // Doesn't work well for B - should never use action invs for preconds

      // gens are & of postconds  att = val  and remote invocations  role2.m(p)
      // First kind become local, others global: 
      if (gens != null && gens.size() > 0)
      { Expression gen = (Expression) gens.get(0);
      
        if (gen != null) 
        { // Vector gbls = splitConjuncts(ebe,eq,g,gen); 
          // res.addAll(gbls);
          Constraint gcon = new Constraint(ebe,eq,g,gen,new Vector());
          gcon.setOwner(this); 
          gcon.setActionInv();  // I assume
          // gcon.typeCheck(types,entities); 
          res.add(gcon);  
        }
      }       
    } 
    return res; 
  } 

  // Why is this in Entity.java?:
  private static Vector splitConjuncts(BehaviouralFeature ebe,Expression eq,
                                Expression g, Expression gen)
  { if (gen instanceof BinaryExpression)
    { BinaryExpression bgen = (BinaryExpression) gen; 
      if (bgen.operator.equals("&"))
      { Vector spl = splitConjuncts(ebe,eq,g,bgen.left); 
        Vector spr = splitConjuncts(ebe,eq,g,bgen.right);
        spl.addAll(spr); 
        return spl; 
      } 
      else // shouldn't happen yet
      { return splitConjuncts(ebe,eq,g,bgen.left); }
    }
    Vector res = new Vector();  
    res.add(new Constraint(ebe,eq,g,gen,new Vector()));
    return res;  
  }  

  private Vector deduceParameters(String op)
  { Vector pars = new Vector(); 
    for (int i = 0; i < attributes.size(); i++)
    { Attribute att = (Attribute) attributes.get(i); 
      String nme = att.getName(); 
      if (("set" + nme).equals(op))
      { Attribute par = 
          new Attribute(nme + "xx",att.getType(),ModelElement.INTERNAL);
        pars.add(par); 
        break; 
      } 
    } 
    for (int i = 0; i < associations.size(); i++)
    { Association ast = (Association) associations.get(i); 
      Entity e2 = ast.getEntity2(); 
      String nme = ast.getRole2(); 
      
      if (("set" + nme).equals(op))
      { Attribute par; 
        if (ast.getCard2() == ONE)
        { par = 
            new Attribute(nme + "xx",new Type(e2),ModelElement.INTERNAL);
        } 
        else 
        { par = 
            new Attribute(nme + "xx",new Type("Set",null),ModelElement.INTERNAL);
          par.setElementType(new Type(e2)); 
        } // Sequence if ordered
        pars.add(par); 
      } 
      else if (("add" + nme).equals(op) ||
               ("remove" + nme).equals(op))
      { Attribute par = 
          new Attribute(nme + "xx",new Type(e2),ModelElement.INTERNAL);
        pars.add(par); 
      } 
    }
    // otherwise, the parameters of the operation 
    return pars; 
  }         

  public AbsMorphism findRefinement()
  { // checks if it matches superclass statemachine, or that of the closest 
    // superclass with a statemachine
    AbsMorphism res = new AbsMorphism(); 

    if (behaviour == null) 
    { System.out.println("No statemachine defined for " + this); 
      return res;
    }
    if (superclass == null)
    { System.out.println("No superclass of " + this); 
      return res;
    }
    Entity sup = superclass; 
    while (sup.behaviour == null)
    { sup = sup.superclass; 
      if (sup == null)
      { System.out.println("No ancestor statemachine defined for " + this); 
        return res;
      }
    }
    boolean ok = res.build_map(sup.behaviour,behaviour); 
    if (ok) 
    { System.out.println("Found refinement map: "); 
      res.print_morphism(); 
      ok = res.checkTotality(); 
      ok = ok && res.checkCompleteness(); 
    } 
    else 
    { System.out.println("No refinement from " + sup + " to " + this); } 
    return res; 
  } 

  private void addRunStates(Statemachine sm)
  { Vector states = sm.getStates(); 
    String attnme = "run_state"; 
    Type intType = new Type("int",null); 
    Attribute att = new Attribute(attnme,intType,ModelElement.INTERNAL); 
    att.setInitialValue("0"); 
    att.setInitialExpression(new BasicExpression("0")); 
    if (attributes.contains(att)) { } 
    else 
    { attributes.add(att); 
      att.setEntity(this);
    }
    for (int i = 0; i < states.size(); i++) 
    { State st = (State) states.get(i); 
      Attribute statt = new Attribute(st.label,intType,ModelElement.INTERNAL); 
      statt.setFrozen(true); 
      statt.setInstanceScope(false); 
      statt.setInitialValue("" + i); 
      statt.setInitialExpression(new BasicExpression("" + i)); 
      if (attributes.contains(statt)) { } 
      else 
      { attributes.add(statt); 
        statt.setEntity(this);
      } 
    }      // have special treatment as constants
    Vector localvars = sm.getAttributes(); 
    for (int j = 0; j < localvars.size(); j++) 
    { Attribute at = (Attribute) localvars.get(j); 
      at.setEntity(this); 
      attributes.add(at); 
    } 
  } 

  public void makeSingleton()
  { cardinality = "1"; 
    setSingleton(true); 
    // add method inst() which generates/returns the instance
    Attribute instanceE = new Attribute("instance_" + getName(),
                                        new Type(this),ModelElement.INTERNAL); 
    instanceE.setEntity(this); 
    instanceE.setInstanceScope(false); 
    attributes.add(instanceE); 
  } 

  public Vector entityInstanceAttributes()
  { Vector res = new Vector(); 
    for (int i = 0; i < attributes.size(); i++) 
    { Attribute att = (Attribute) attributes.get(i); 
      if (att.isEntityInstance())
      { Association ast = new Association(this,att); 
        res.add(ast); 
      } 
    } 
    return res; 
  } 

  public Vector allDefinedEntityInstanceAttributes()
  { Vector res = new Vector(); 
    if (superclass == null) 
    { return entityInstanceAttributes(); } 
    else 
    { res = superclass.allDefinedEntityInstanceAttributes(); 
      res.addAll(entityInstanceAttributes()); 
    } 
    return res; 
  } 

 public String dialogDefinition(String dname)
 { String header = "import java.awt.*;\n" +
     "import java.awt.event.*;\n" +
     "import javax.swing.*;\n" +
     "import javax.swing.event.*;\n" +
     "import javax.swing.border.Border;\n" +
     "import java.util.EventObject;\n" +
     "import java.util.Vector;\n" +
     "import java.io.*; \n\n";

 String dclass = 
  "class " + dname + " extends JDialog\n" +
  "{ private JPanel bottom;\n" +
  "  private JButton okButton, cancelButton;\n" +
  "  private DialogPanel dialogPanel;\n";
  
  for (int i = 0; i < attributes.size(); i++)
  { Attribute att = (Attribute) attributes.get(i);
    String attnme = att.getName();
    String jtype = att.getJavaType();

    dclass = dclass +
      "  private " + jtype + " default" + attnme + ";\n" +
      "  private " + jtype + " new" + attnme + ";\n\n";
  }
   
  dclass = dclass +
  "  public " + dname + "(JFrame owner)\n" +
  "  { super(owner, true);\n" +
  "    setTitle(\"" + dname + "\");\n" +
  "    okButton = new JButton(\"Ok\");\n" +
  "    cancelButton = new JButton(\"Cancel\");\n" +
  "    ButtonHandler bHandler = new ButtonHandler();\n" +
  "    okButton.addActionListener(bHandler);\n" +
  "    cancelButton.addActionListener(bHandler);\n" +
  "    bottom = new JPanel();\n" +
  "    bottom.add(okButton);\n" +
  "    bottom.add(cancelButton);\n" +
  "    bottom.setBorder(BorderFactory.createEtchedBorder());\n" + 
  "    dialogPanel = new DialogPanel();\n" +
  "    getContentPane().setLayout(new BorderLayout());\n" +
  "    getContentPane().add(bottom, BorderLayout.SOUTH);\n" +
  "    getContentPane().add(dialogPanel, BorderLayout.CENTER);\n" + 
  "  }\n";

  String pars = "";
  String pars2 = "";
  String parnames = "";
  String copypars = "";
  String copynewpars = "";
  String getops = "";
  String guiattdecs = "";
  String guiattdefs = "";
  String attinits = "";
  String setfieldokpars = "";
  String setfieldnotokpars = "";

  for (int i = 0; i < attributes.size(); i++)
  { Attribute att = (Attribute) attributes.get(i);
    String attnme = att.getName();
    String jtype = att.getJavaType();
    pars = pars + jtype + " " + attnme + "x";
    if (jtype.equals("int") || jtype.equals("long") || jtype.equals("double"))
    { pars2 = pars2 + "String " + " " + attnme + "x"; }
    else
    { pars2 = pars2 + jtype + " " + attnme + "x"; }
    parnames = parnames + attnme + "x";
    copypars = copypars + "    default" + attnme + " = " +
                        attnme + "x;\n";
    copynewpars = copynewpars + "    " +
       att.guiconversioncode();
    getops = getops +
      "  public " + jtype + " get" + attnme +
      "() { return new" + attnme + "; }\n";
    guiattdecs = guiattdecs + "    " + att.guidec() + "\n";
    guiattdefs = guiattdefs + "    " + att.guidef() + "\n";
    attinits = attinits + "      " + att.guiattinit() + "\n";
    setfieldokpars = setfieldokpars + att.guifieldpar1();
    setfieldnotokpars = setfieldnotokpars + att.guifieldpar2();

    if (i < attributes.size() - 1)
    { pars = pars + ",";
      pars2 = pars2 + ",";
      parnames = parnames + ","; 
      setfieldokpars = setfieldokpars + ",";
      setfieldnotokpars = setfieldnotokpars + ",";
    }
  }

  dclass = dclass + 
    "  public void setOldFields(" + pars + ")\n" +
    "  { " + copypars + 
    "    dialogPanel.setOldFields(" + parnames + ");\n" + 
    "  }\n\n";

  dclass = dclass + 
    "  public void setFields(" + pars2 + ")\n" +
    "  { " + copynewpars +  
    "  }\n\n" + getops + "\n\n" +

    "  class DialogPanel extends JPanel\n" + 
    "  { " + guiattdecs + "\n\n" +
    "    public DialogPanel() { \n" + guiattdefs + "    }\n\n";

  dclass = dclass +
    "    public void setOldFields(" + pars + ")\n" +
    "    { " + attinits + "    }\n\n";

  int x = 10;
  int y = 10;
  String layoutdefs = "";
  for (int i = 0; i < attributes.size(); i++)
  { Attribute att = (Attribute) attributes.get(i);
    String attnme = att.getName();
    Type t = att.getType();
    String tname = t.getName();
    if (tname == null)  { }
    else if (tname.equals("String") || tname.equals("int") || tname.equals("long") || 
                tname.equals("double"))
    { layoutdefs = layoutdefs +
        "    " + attnme + "Label.setBounds(" + x + "," + y +
        "," + 60 + "," + 30 + ");\n" +
        "    " + attnme + "Field.setBounds(" + 70 + "," + (y+5) +
        "," + 270 + "," + 20 + ");\n";
      y = y+30;
    }
    else if (tname.equals("boolean"))
    { layoutdefs = layoutdefs +
        "    " + attnme + "Panel.setBounds(" + x + "," + (y + 10) + 
        "," + 330 + "," + 50 + ");\n";
      y = y+60;
    }
  }
    
  String lops =
    "    public Dimension getPreferredSize()\n" +
    "    { return new Dimension(350," + (y + 10) + "); }\n\n" +

    "    public Dimension getMinimumSize()\n" +
    "    { return new Dimension(350," + (y + 10) + "); }\n\n" +

    "    public void doLayout()\n" + 
    "    { " + layoutdefs + 
    "    }\n }\n\n";

  dclass = dclass + lops + 
    "  class ButtonHandler implements ActionListener\n" +
    "  { public void actionPerformed(ActionEvent ev)\n" +
    "    { JButton button = (JButton) ev.getSource();\n" +
    "      String label = button.getText();\n" +
    "      if (\"Ok\".equals(label))\n" +
    "      { setFields(" + setfieldokpars + ");\n" + 
    "      }\n" +
    "     else\n" + 
    "     { setFields(" + setfieldnotokpars + "); }\n" +
 
    "     setVisible(false);\n" + 
    "    }\n" + 
    "  }\n" +
    "}\n";
    return dclass;
  }

  public String getCSharpAddObjOp()
  { if (isAbstract() || isInterface()) { return ""; } 
    if (hasStereotype("external") || hasStereotype("externalApp")) { return ""; } 

    String cname = getName();
    String cx = cname.toLowerCase() + "x";
    String res = "  if (c.Equals(\"" + cname + "\"))\n" +
      "  { " + cname + " " + cx + " = new " + cname + "();\n" +
      "     objectmap[a] = " + cx + ";\n" +
      "     classmap[a] = c;\n" +
      "     add" + cname + "(" + cx + ");\n" +
      "    return;\n" + 
      "  }\n";
    return res;
  } 

  public String getCPPAddObjOp()
  { if (isAbstract() || isInterface()) { return ""; } 
    if (hasStereotype("external") || hasStereotype("externalApp")) { return ""; } 

    String cname = getName();
    String cx = cname.toLowerCase() + "x";
    String res = " if (c == \"" + cname + "\")\n" +
      "    { " + cname + "* " + cx + " = new " + cname + "();\n" +
      "       objectmap[a] = " + cx + ";\n" +
      "       classmap[a] = c;\n" +
      "       add" + cname + "(" + cx + ");\n" +
      "      return;\n" + 
      "    }\n";
    return res;
  } 


  public String generateSaveModel1()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ex = ename.toLowerCase() + "x_";
    res = "  for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
             "  { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" + 
             "    out.println(\"" + ex  + "\" + _i + \" : " + ename + "\");\n";

    Vector allAttributes = new Vector(); 
    allAttributes.addAll(attributes); 
    allAttributes.addAll(allInheritedAttributes()); 

    for (int j = 0; j < allAttributes.size(); j++)
    { Attribute att = (Attribute) allAttributes.get(j);
      if (att.isMultiple()) 
      { String attname = att.getName(); 
        String r = ename.toLowerCase() + "_" + attname;
        String quote1 = "\"\\\"\" + ";  
        String quote2 = "\"\\";  
        if ("String".equals(att.getElementType() + "")) { } 
        else 
        { quote1 = ""; 
          quote2 = ""; 
        } 

        res = res + "    List " + r + " = " + ex + ".get" + attname + "();\n" + 
                    "    for (int _j = 0; _j < " + r + ".size(); _j++)\n" +
                    "    { out.println(" + quote1 + r + ".get(_j) + " + quote2 + "\" : " + 
                           ex + "\" + _i + \"." + attname + "\");\n" + 
                    "    }\n"; 
      }  
      else if (att.isEntityInstance())
      { // treat as an association 
      } 
      else 
      { String attname = att.getName();
        Type attt = att.getType(); 
        if ("String".equals(attt + ""))
        { res = res + "    out.println(\"" + ex + "\" + _i + \"." + attname + 
                  " = \\\"\" + " + ex + ".get" + attname + "() + \"\\\"\");\n";
        }
        else 
        { res = res + "    out.println(\"" + ex + "\" + _i + \"." + attname + 
                  " = \" + " + ex + ".get" + attname + "());\n";
        }
      } 
    }

    return res + "  }\n\n"; 
  } // works for Java 6 and 7 also 

  public String generateSaveModel1CSharp()
  { if (isAbstract() || isInterface() || hasStereotype("external") || 
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "_s";
    String ex = ename.toLowerCase() + "x_";
    res = "  for (int _i = 0; _i < " + es + ".Count; _i++)\n" +
             "  { " + ename + " " + ex + " = (" + ename + ") " + es + "[_i];\n" + 
             "    outfile.WriteLine(\"" + ex  + "\" + _i + \" : " + ename + "\");\n";

    Vector allAttributes = new Vector(); 
    allAttributes.addAll(attributes); 
    allAttributes.addAll(allInheritedAttributes()); 

    for (int j = 0; j < allAttributes.size(); j++)
    { Attribute att = (Attribute) allAttributes.get(j);
      if (att.isMultiple() || att.isStatic()) 
      { continue; } 
      String attname = att.getName();
      Type attt = att.getType(); 
      if ("String".equals(attt + ""))
      { res = res + "    outfile.WriteLine(\"" + ex + "\" + _i + \"." + attname + 
                  " = \\\"\" + " + ex + ".get" + attname + "() + \"\\\"\");\n";
      }
      else 
      { res = res + "    outfile.WriteLine(\"" + ex + "\" + _i + \"." + attname + 
                  " = \" + " + ex + ".get" + attname + "());\n";
      }
    }

    return res + "  }\n\n"; 
  } 

  public String generateSaveModel1CPP()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "_s";
    String ex = ename.toLowerCase() + "x_";
    res = "  for (int _i = 0; _i < " + es + "->size(); _i++)\n" +
             "  { " + ename + "* " + ex + " = (*" + es + ")[_i];\n" + 
             "    outfile << \"" + ex  + "\" << (_i+1) << \" : " + ename + "\" << endl;\n";

    Vector allAttributes = new Vector(); 
    allAttributes.addAll(attributes); 
    allAttributes.addAll(allInheritedAttributes()); 

    for (int j = 0; j < allAttributes.size(); j++)
    { Attribute att = (Attribute) allAttributes.get(j);
      if (att.isMultiple()) 
      { continue; } 
      String attname = att.getName();
      Type attt = att.getType(); 
      if ("String".equals(attt + ""))
      { res = res + "    outfile << \"" + ex + "\" << (_i+1) << \"." + attname + 
                  " = \\\"\" << " + ex + "->get" + attname + "() << \"\\\"\" << endl;\n";
      }
      else 
      { res = res + "    outfile << \"" + ex + "\" << (_i+1) << \"." + attname + 
                  " = \" << " + ex + "->get" + attname + "() << endl;\n";
      }
    }

    return res + "  }\n\n"; 
  } 


  public String generateSaveModel2()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ex = ename.toLowerCase() + "x_";

    res = "  for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
             "  { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" ;

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 
    allAssociations.addAll(allDefinedEntityInstanceAttributes()); 

    if (allAssociations.size() == 0) 
    { return ""; } 

    for (int j = 0; j < allAssociations.size(); j++)
    { Association ast = (Association) allAssociations.get(j);
      if (ast.isQualified()) { continue; } 

      String astname = ast.getRole2();
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "s"; 
      String e2x = e2name.toLowerCase() + "x_"; 
      // If ent2.isAbstract() print out elements based on their concrete
      // classes, not ent2. 
      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            res = res + "    if (" + ex + ".get" + astname + "()" + 
                        " instanceof " + leafnme + ")\n" + 
                        "    { out.println(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + lx + "\" + " + ls + ".indexOf(" + ex + ".get" + 
                  astname + "())); } \n";
          }
        }
        else 
        { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
          res = 
           res + "    List " + r + " = " + ex + ".get" + astname + "();\n" + 
                 "    for (int _k = 0; _k < " + r + ".size(); _k++)\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            res = res + " if (" + r + ".get(_k)" + 
                      " instanceof " + leafnme + ")\n" + 
                      "      { out.println(\"" + lx + "\" + " + ls + ".indexOf(" + 
                  r + ".get(_k)) + \" : " + 
                          ex + "\" + _i + \"." + astname + "\"); }\n"; 
          }
          res = res + "  }\n"; 
        }  
      } 
      else if (ast.getCard2() == ONE)
      { res = res + "    out.println(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + e2x + "\" + " + e2s + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n";
      }
      else 
      { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
        res = 
         res + "    List " + r + " = " + ex + ".get" + 
                  astname + "();\n" + 
                    "    for (int _j = 0; _j < " + r + ".size(); _j++)\n" +
                    "    { out.println(\"" + e2x + "\" + " +
                           e2s + ".indexOf(" + r + ".get(_j)) + \" : " + 
                           ex + "\" + _i + \"." + astname + "\");\n" + 
                    "    }\n"; 
      }  
    } 

    return res + "  }\n";
  } 

  public String generateSaveModel2Java6()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ex = ename.toLowerCase() + "x_";

    res = "  for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
             "  { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" ;

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    if (allAssociations.size() == 0) 
    { return ""; } 

    for (int j = 0; j < allAssociations.size(); j++)
    { Association ast = (Association) allAssociations.get(j);
      if (ast.isQualified()) { continue; } 

      String astname = ast.getRole2();
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "s"; 
      String e2x = e2name.toLowerCase() + "x_"; 
      // If ent2.isAbstract() print out elements based on their concrete
      // classes, not ent2. 
      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + "    if (" + ex + ".get" + astname + "()" + 
                        " instanceof " + leafnme + ")\n" + 
                        "    { out.println(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + lx + "\" + " + ls + ".indexOf(" + ex + ".get" + 
                  astname + "())); } \n";
          }
        }
        else  // order isn't preserved. 
        { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
          res = 
           res + "    Collection " + r + " = " + ex + ".get" + astname + "();\n" + 
                 "    for (Object _k : " + r + ")\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + " if (_k instanceof " + leafnme + ")\n" + 
                      "      { out.println(\"" + lx + "\" + " + ls + ".indexOf(_k) + \" : " + 
                          ex + "\" + _i + \"." + astname + "\"); }\n"; 
          }
          res = res + "  }\n"; 
        }  
      } 
      else if (ast.getCard2() == ONE)
      { res = res + "    out.println(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + e2x + "\" + " + e2s + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n";
      }
      else // order isn't preserved 
      { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
        res = 
         res + "    Collection " + r + " = " + ex + ".get" + astname + "();\n" + 
                    "    for (Object _j : " + r + ")\n" +
                    "    { out.println(\"" + e2x + "\" + " +
                           e2s + ".indexOf(_j) + \" : " + 
                           ex + "\" + _i + \"." + astname + "\");\n" + 
                    "    }\n"; 
      }  
    } 

    return res + "  }\n";
  } 

  public String generateSaveModel2Java7()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ex = ename.toLowerCase() + "x_";

    res = "  for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
             "  { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" ;

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    if (allAssociations.size() == 0) 
    { return ""; } 

    for (int j = 0; j < allAssociations.size(); j++)
    { Association ast = (Association) allAssociations.get(j);
      if (ast.isQualified()) { continue; } 

      String astname = ast.getRole2();
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "s"; 
      String e2x = e2name.toLowerCase() + "x_"; 
      // If ent2.isAbstract() print out elements based on their concrete
      // classes, not ent2. 
      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + "    if (" + ex + ".get" + astname + "()" + 
                        " instanceof " + leafnme + ")\n" + 
                        "    { out.println(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + lx + "\" + " + ls + ".indexOf(" + ex + ".get" + 
                  astname + "())); } \n";
          }
        }
        else // order isn't preserved. 
        { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
          res = 
           res + "    Collection<" + e2name + "> " + r + " = " + ex + ".get" + astname + "();\n" + 
                 "    for (Object _k : " + r + ")\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + " if (_k instanceof " + leafnme + ")\n" + 
                      "      { out.println(\"" + lx + "\" + " + ls + ".indexOf(_k) + \" : " + 
                          ex + "\" + _i + \"." + astname + "\"); }\n"; 
          }
          res = res + "  }\n"; 
        }  
      } 
      else if (ast.getCard2() == ONE)
      { res = res + "    out.println(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + e2x + "\" + " + e2s + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n";
      }
      else // order isn't preserved. 
      { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
        res = 
         res + "    Collection<" + e2name + "> " + r + " = " + ex + ".get" + astname + "();\n" + 
                    "    for (Object _j : " + r + ")\n" +
                    "    { out.println(\"" + e2x + "\" + " +
                           e2s + ".indexOf(_j) + \" : " + 
                           ex + "\" + _i + \"." + astname + "\");\n" + 
                    "    }\n"; 
      }  
    } 

    return res + "  }\n";
  } 

  public String generateSaveModel2CSharp()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "_s";
    String ex = ename.toLowerCase() + "x_";

    res = "  for (int _i = 0; _i < " + es + ".Count; _i++)\n" +
             "  { " + ename + " " + ex + " = (" + ename + ") " + es + "[_i];\n" ;

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    if (allAssociations.size() == 0) 
    { return ""; } 

    for (int j = 0; j < allAssociations.size(); j++)
    { Association ast = (Association) allAssociations.get(j);
      if (ast.isQualified()) { continue; } 

      String astname = ast.getRole2();
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "_s"; 
      String e2x = e2name.toLowerCase() + "x_"; 
      // If ent2.isAbstract() print out elements based on their concrete
      // classes, not ent2. 
      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "_s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + "    if (" + ex + ".get" + astname + "()" + 
                        " is " + leafnme + ")\n" + 
                        "    { outfile.WriteLine(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + lx + "\" + " + ls + ".IndexOf(" + ex + ".get" + 
                  astname + "())); } \n";
          }
        }
        else 
        { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
          res = 
           res + "    ArrayList " + r + " = " + ex + ".get" + astname + "();\n" + 
                 "    for (int _k = 0; _k < " + r + ".Count; _k++)\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "_s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + " if (" + r + "[_k]" + 
                      " is " + leafnme + ")\n" + 
                      "      { outfile.WriteLine(\"" + lx + "\" + " + ls + ".IndexOf(" + 
                  r + "[_k]) + \" : " + 
                          ex + "\" + _i + \"." + astname + "\"); }\n"; 
          }
          res = res + "  }\n"; 
        }  
      } 
      else if (ast.getCard2() == ONE)
      { res = res + "    outfile.WriteLine(\"" + ex + "\" + _i + \"." + astname + 
                  " = " + e2x + "\" + " + e2s + ".IndexOf(((" + ename + ") " + es + "[_i]).get" + 
                  astname + "()));\n";
      }
      else 
      { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
        res = 
         res + "    ArrayList " + r + " = " + ex + ".get" + 
                  astname + "();\n" + 
                    "    for (int _j = 0; _j < " + r + ".Count; _j++)\n" +
                    "    { outfile.WriteLine(\"" + e2x + "\" + " +
                           e2s + ".IndexOf(" + r + "[_j]) + \" : " + 
                           ex + "\" + _i + \"." + astname + "\");\n" + 
                    "    }\n"; 
      }  
    } 

    return res + "  }\n";
  } 

  public String generateSaveModel2CPP()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "_s";
    String ex = ename.toLowerCase() + "x_";

    res = "  for (int _i = 0; _i < " + es + "->size(); _i++)\n" +
          "  { " + ename + "* " + ex + " = (*" + es + ")[_i];\n" ;

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    if (allAssociations.size() == 0) 
    { return ""; } 

    for (int j = 0; j < allAssociations.size(); j++)
    { Association ast = (Association) allAssociations.get(j);
      if (ast.isQualified()) { continue; } 

      
      String astname = ast.getRole2();
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "_s"; 
      String e2x = e2name.toLowerCase() + "x_";

      String e2roletype = ""; 
      if (ast.isOrdered())
      { e2roletype = "vector<" + e2name + "*>"; }
      else 
      { e2roletype = "set<" + e2name + "*>"; }
 
      // If ent2.isAbstract() print out elements based on their concrete
      // classes, not ent2. 
      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "_s"; 
            String lx = leafnme.toLowerCase() + "x_";
            if (k > 0) 
            { res = res + " else"; }   
            res = res + " if (UmlRsdsLib<" + e2name + "*>::isIn(" + ex + "->get" + astname + "(), " + 
                        "(vector<" + e2name + "*>*) " + ls + "))\n" + 
                        "    { outfile << \"" + ex + "\" << (_i+1) << \"." + astname + 
                  " = " + lx + "\" << UmlRsdsLib<" + e2name + "*>::indexOf(" + ex + "->get" + 
                  astname + "(), (vector<" + e2name + "*>*) " + ls + ") << endl; } \n";
          }
        }
        else 
        { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
          res = 
           res + "    " + e2roletype + "* " + r + " = " + ex + "->get" + astname + "();\n" + 
                 "    for (" + e2roletype + "::iterator _k = " + r + "->begin(); _k != " + r + "->end(); _k++)\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "_s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + " if (UmlRsdsLib<" + e2name + "*>::isIn(*_k, (vector<" + e2name + "*>*) " + ls + "))\n" + 
                      "      { outfile << \"" + lx + "\" << UmlRsdsLib<" + e2name + "*>::indexOf(" + 
                      "*_k, (vector<" + e2name + "*>*) " + ls + ") << \" : " + 
                          ex + "\" << (_i+1) << \"." + astname + "\" << endl; }\n"; 
          }
          res = res + "  }\n"; 
        }  
      } 
      else if (ast.getCard2() == ONE)
      { res = res + "    outfile << \"" + ex + "\" << (_i+1) << \"." + astname + 
                  " = " + e2x + "\" << UmlRsdsLib<" + e2name + "*>::indexOf((*" + es + ")[_i]->get" + 
                  astname + "(), " + e2s + ") << endl;\n";
      }
      else 
      { String r = ename.toLowerCase() + "_" + astname + "_" + e2name; 
        res = 
         res + "   " + e2roletype + "* " + r + " = " + ex + "->get" + astname + "();\n" + 
                    "    for (" + e2roletype + "::iterator _j = " + r + "->begin(); _j != " + r + "->end(); _j++)\n" +
                    "    { outfile << \"" + e2x + "\" << UmlRsdsLib<" + e2name + "*>::indexOf(" + 
                           "*_j, " + e2s + ") << \" : " + 
                           ex + "\" << (_i+1) << \"." + astname + "\" << endl;\n" + 
                    "    }\n"; 
      }  
    } 

    return res + "  }\n";
  } 


  public String getCSharpAddRoleOp()
  { if (isAbstract() || isInterface()) { return ""; } 

    String cname = getName();
    String cx = cname.toLowerCase() + "x";
    String res = "";
    Vector assts = allDefinedAssociations(); 

    for (int i = 0; i < assts.size(); i++)
    { Association ast = (Association) assts.get(i);
      if (ast.isQualified()) { } 
      else if (ast.getCard2() != ONE) 
      { String ent2 = ast.getEntity2() + "";
        String r2 = ast.getRole2();
        String e2x = ent2.toLowerCase() + r2 + "x";
        res = res + 
          "  if (\"" + cname + "\".Equals(classmap[a]) && role.Equals(\"" + r2 + "\"))\n" +
          "  { " + cname + " " + cx + " = (" + cname + ") objectmap[a];\n" +
          "    " + ent2 + " " + e2x + " = (" + ent2 + ") objectmap[b];\n" +
          "    add" + r2 + "(" + cx + "," + e2x + ");\n" +
          "    return;\n" +
          "  }\n"; 
      }
    }
    return res;
  }

  public String getCPPAddRoleOp()
  { if (isAbstract() || isInterface()) { return ""; } 

    String cname = getName();
    String cx = cname.toLowerCase() + "x";
    String res = "";
    Vector assts = allDefinedAssociations(); 

    for (int i = 0; i < assts.size(); i++)
    { Association ast = (Association) assts.get(i);
      if (ast.isQualified()) { } 
      else if (ast.getCard2() != ONE) 
      { String ent2 = ast.getEntity2() + "";
        String r2 = ast.getRole2();
        String e2x = ent2.toLowerCase() + r2 + "x";
        res = res + 
          "  if (\"" + cname + "\" == classmap[a] && role == \"" + r2 + "\")\n" +
          "  { " + cname + "* " + cx + " = (" + cname + "*) objectmap[a];\n" +
          "    " + ent2 + "* " + e2x + " = (" + ent2 + "*) objectmap[b];\n" +
          "    " + cx + "->add" + r2 + "(" + e2x + ");\n" +
          "    return;\n" +
          "  }\n"; 
      }
    }
    return res;
  }


  public String getCSharpSetFeatureOp()
  { if (isAbstract() || isInterface()) { return ""; } 

    String cname = getName();
    String cx = cname.toLowerCase() + "x";
    String res = "";
    Vector atts = allDefinedAttributes(); 
    Vector assts = allDefinedAssociations(); 

    for (int j = 0; j < atts.size(); j++)
    { Attribute att = (Attribute) atts.get(j);
      if (att.isStatic()) { continue; } 
      String aname = att.getName();
      Type t = att.getType();
      String tname = t.getName();
      String valc = "val";
      if ("String".equals(tname))
      { valc = "val.Substring(1,val.Length-2)"; }
      else if ("double".equals(tname))
      { valc = "double.Parse(val)"; }
      else if ("boolean".equals(tname))
      { valc = "val.Equals(\"true\")"; }
      else if ("long".equals(tname))
      { valc = "Int64.Parse(val)"; }
      else 
      { valc = "int.Parse(val)"; }
      // enumerations, long?

      res = res + 
        "  if (\"" + cname + "\".Equals(classmap[a]) && f.Equals(\"" + aname + "\"))\n" +
        "  { " + cname + " " + cx + " = (" + cname + ") objectmap[a];\n" +
        "    set" + aname + "(" + cx + "," + valc + ");\n" +
        "    return;\n" +
        "  }\n"; 
    }

    for (int i = 0; i < assts.size(); i++)
    { Association ast = (Association) assts.get(i);
     if (ast.isQualified()) { } 
     else if (ast.getCard2() == ONE) 
      { String ent2 = ast.getEntity2() + "";
        String r2 = ast.getRole2();
        String fx = ent2.toLowerCase() + r2 + "x";
        res = res + 
          "  if (\"" + cname + "\".Equals(classmap[a]) && f.Equals(\"" + r2 + "\"))\n" +
          "  { " + cname + " " + cx + " = (" + cname + ") objectmap[a];\n" +
          "    " + ent2 + " " + fx + " = (" + ent2 + ") objectmap[val];\n" +
          "    set" + r2 + "(" + cx + "," + fx + ");\n" +
          "    return;\n" +
          "  }\n"; 
      }
    }
    return res;
  }


  public String getCPPSetFeatureOp()
  { if (isAbstract() || isInterface()) { return ""; } 

    String cname = getName();
    String cx = cname.toLowerCase() + "x";
    String res = "";
    Vector atts = allDefinedAttributes(); 
    Vector assts = allDefinedAssociations(); 

    for (int j = 0; j < atts.size(); j++)
    { Attribute att = (Attribute) atts.get(j);
      String aname = att.getName();
      Type t = att.getType();
      String tname = t.getName();
      String valc = "val";
      if ("String".equals(tname))
      { valc = "val.substr(1,val.length()-2)"; }
      else if ("double".equals(tname))
      { valc = "std::stod(val)"; }
      else if ("boolean".equals(tname))
      { valc = "(val == \"true\")"; }
      else if ("long".equals(tname))
      { valc = "std::stol(val)"; }
      else 
      { valc = "std::stoi(val)"; }
      // enumerations?

      res = res + 
        "  if (\"" + cname + "\" == classmap[a] && f == \"" + aname + "\")\n" +
        "  { " + cname + "* " + cx + " = (" + cname + "*) objectmap[a];\n" +
        "    " + cx + "->set" + aname + "(" + valc + ");\n" +
        "    return;\n" +
        "  }\n"; 
    }

    for (int i = 0; i < assts.size(); i++)
    { Association ast = (Association) assts.get(i);
     if (ast.isQualified()) { } 
     else if (ast.getCard2() == ONE) 
      { String ent2 = ast.getEntity2() + "";
        String r2 = ast.getRole2();
        String fx = ent2.toLowerCase() + r2 + "x";
        res = res + 
          "  if (\"" + cname + "\" == classmap[a] && f == \"" + r2 + "\")\n" +
          "  { " + cname + "* " + cx + " = (" + cname + "*) objectmap[a];\n" +
          "    " + ent2 + "* " + fx + " = (" + ent2 + "*) objectmap[val];\n" +
          "    " + cx + "->set" + r2 + "(" + fx + ");\n" +
          "    return;\n" +
          "  }\n"; 
      }
    }
    return res;
  }


  public String xsiSaveModel()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ex = ename.toLowerCase() + "x_";
    res = "    for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
          "    { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" + 
          "       out.print(\"<" + es  + " xsi:type=\\\"My:" + ename + "\\\"\");\n";

    Vector allAttributes = new Vector(); 
    allAttributes.addAll(attributes); 
    allAttributes.addAll(allInheritedAttributes()); 

    for (int j = 0; j < allAttributes.size(); j++)
    { Attribute att = (Attribute) allAttributes.get(j);
      if (att.isMultiple()) 
      { continue; } 
      String attname = att.getName();
      res = res + "    out.print(\" " + attname + "=\\\"\" + " + ex + ".get" + attname + "() + \"\\\" \");\n";
    }

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    for (int y = 0; y < allAssociations.size(); y++) 
    { Association ast = (Association) allAssociations.get(y); 
      if (ast.isQualified()) { continue; } 

      String astname = ast.getRole2(); 
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "s"; 

      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + "    if (" + ex + ".get" + astname + "()" + 
                        " instanceof " + leafnme + ")\n" + 
                        "    {   out.print(\" " + astname + "=\\\"\");\n" + 
                        "    out.print(\"//@" + ls + ".\" + " + 
                        ls + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n" + 
                        "    out.print(\"\\\"\"); }\n";
          }
        }
        else 
        { String r = ename.toLowerCase() + "_" + astname; 
          res = 
           res + "    out.print(\" " + astname + " = \\\"\");\n" +  
                 "    List " + r + " = " + ex + ".get" + astname + "();\n" + 
                 "    for (int _k = 0; _k < " + r + ".size(); _k++)\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + 
                      "      if (" + r + ".get(_k)" + " instanceof " + leafnme + ")\n" + 
                      "      { out.print(\" //@" + ls + ".\" + " + ls + ".indexOf(" + 
                      r + ".get(_k)));\n" + "    }\n";  
          }
          res = res + "  }\n" + "    out.print(\"\\\"\");\n"; 
        }  
      }
      else if (ast.getCard2() == ONE)
      { res = res + "    out.print(\" " + astname + "=\\\"\");\n" + 
                    "    out.print(\"//@" + e2s + ".\" + " + 
                        e2s + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n" + 
                    "    out.print(\"\\\"\");\n";
      }
      else      
      { String r = ename.toLowerCase() + "_" + astname; 
        res = 
         res + "    out.print(\" " + astname + " = \\\"\");\n" +  
               "    List " + r + " = " + ex + ".get" + astname + "();\n" + 
               "    for (int _j = 0; _j < " + r + ".size(); _j++)\n" +
               "    { out.print(\" //@" + e2s + ".\" + " +
                      e2s + ".indexOf(" + r + ".get(_j)));\n" + 
               "    }\n" + 
               "    out.print(\"\\\"\");\n"; 
      }  
    }
    return res +  "    out.println(\" />\");\n  }\n\n"; 
  } 

  public String xmiSaveModel(String domain)
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ex = ename.toLowerCase() + "x_";
    res = "    for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
          "    { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" + 
          "       out.print(\"<" + es  + " xsi:type=\\\"" + domain + ":" + ename + "\\\"\");\n";

    Vector allAttributes = new Vector(); 
    allAttributes.addAll(attributes); 
    allAttributes.addAll(allInheritedAttributes()); 

    for (int j = 0; j < allAttributes.size(); j++)
    { Attribute att = (Attribute) allAttributes.get(j);
      if (att.isMultiple()) 
      { continue; } 
      String attname = att.getName();
      res = res + "    out.print(\" " + attname + "=\\\"\" + " + ex + ".get" + attname + "() + \"\\\" \");\n";
    }

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    for (int y = 0; y < allAssociations.size(); y++) 
    { Association ast = (Association) allAssociations.get(y); 
      if (ast.isQualified()) { continue; } 
      if (ast.isAggregation())
      { // elements as sub-nodes
        ast.xmiSaveModel(domain); 
        continue; 
      } 

      String astname = ast.getRole2(); 
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "s"; 

      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            res = res + "    if (" + ex + ".get" + astname + "()" + 
                        " instanceof " + leafnme + ")\n" + 
                        "    {   out.print(\" " + astname + "=\\\"\");\n" + 
                        "    out.print(\"//@" + ls + ".\" + " + 
                        ls + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n" + 
                        "    out.print(\"\\\"\"); }\n";
          }
        }
        else 
        { String r = ename.toLowerCase() + "_" + astname; 
          res = 
           res + "    out.print(\" " + astname + " = \\\"\");\n" +  
                 "    List " + r + " = " + ex + ".get" + astname + "();\n" + 
                 "    for (int _k = 0; _k < " + r + ".size(); _k++)\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + 
                      "      if (" + r + ".get(_k)" + " instanceof " + leafnme + ")\n" + 
                      "      { out.print(\" //@" + ls + ".\" + " + ls + ".indexOf(" + 
                      r + ".get(_k)));\n" + "    }\n";  
          }
          res = res + "  }\n" + "    out.print(\"\\\"\");\n"; 
        }  
      }
      else if (ast.getCard2() == ONE)
      { res = res + "    out.print(\" " + astname + "=\\\"\");\n" + 
                    "    out.print(\"//@" + e2s + ".\" + " + 
                        e2s + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n" + 
                    "    out.print(\"\\\"\");\n";
      }
      else      
      { String r = ename.toLowerCase() + "_" + astname; 
        res = 
         res + "    out.print(\" " + astname + " = \\\"\");\n" +  
               "    List " + r + " = " + ex + ".get" + astname + "();\n" + 
               "    for (int _j = 0; _j < " + r + ".size(); _j++)\n" +
               "    { out.print(\" //@" + e2s + ".\" + " +
                      e2s + ".indexOf(" + r + ".get(_j)));\n" + 
               "    }\n" + 
               "    out.print(\"\\\"\");\n"; 
      }  
    }
    return res +  "    out.println(\" />\");\n  }\n\n"; 
  } 

  public String xsiSaveModelJava6()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ex = ename.toLowerCase() + "x_";
    res = "    for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
          "    { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" + 
          "       out.print(\"<" + es  + " xsi:type=\\\"My:" + ename + "\\\"\");\n";

    Vector allAttributes = new Vector(); 
    allAttributes.addAll(attributes); 
    allAttributes.addAll(allInheritedAttributes()); 

    for (int j = 0; j < allAttributes.size(); j++)
    { Attribute att = (Attribute) allAttributes.get(j);
      if (att.isMultiple()) 
      { continue; } 
      String attname = att.getName();
      res = res + "    out.print(\" " + attname + "=\\\"\" + " + ex + ".get" + attname + "() + \"\\\" \");\n";
    }

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    for (int y = 0; y < allAssociations.size(); y++) 
    { Association ast = (Association) allAssociations.get(y); 
      if (ast.isQualified()) { continue; } 

      String astname = ast.getRole2(); 
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "s"; 

      if (ent2.isAbstract())
      { Vector leafs = ent2.getActualLeafSubclasses(); 
        // System.out.println("Leaf subclasses of " + e2name + " are: " + leafs); 
        if (ast.getCard2() == ONE)
        { for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + "    if (" + ex + ".get" + astname + "()" + 
                        " instanceof " + leafnme + ")\n" + 
                        "    {   out.print(\" " + astname + "=\\\"\");\n" + 
                        "    out.print(\"//@" + ls + ".\" + " + 
                        ls + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n" + 
                        "    out.print(\"\\\"\"); }\n";
          }
        }
        else 
        { String r = ename.toLowerCase() + "_" + astname; 
          res = 
           res + "    out.print(\" " + astname + " = \\\"\");\n" +  
                 "    Collection " + r + " = " + ex + ".get" + astname + "();\n" + 
                 "    for (Object _k : " + r + ")\n" +
                 "    {"; 
          for (int k = 0; k < leafs.size(); k++) 
          { String leafnme = ((Entity) leafs.get(k)).getName();
            String ls = leafnme.toLowerCase() + "s"; 
            String lx = leafnme.toLowerCase() + "x_";  
            if (k > 0) 
            { res = res + " else"; }   
            res = res + 
                      "      if (" + r + ".get(_k)" + " instanceof " + leafnme + ")\n" + 
                      "      { out.print(\" //@" + ls + ".\" + " + ls + ".indexOf(" + 
                      "_k));\n" + "    }\n";  
          }
          res = res + "  }\n" + "    out.print(\"\\\"\");\n"; 
        }  
      }
      else if (ast.getCard2() == ONE)
      { res = res + "    out.print(\" " + astname + "=\\\"\");\n" + 
                    "    out.print(\"//@" + e2s + ".\" + " + 
                        e2s + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n" + 
                    "    out.print(\"\\\"\");\n";
      }
      else      
      { String r = ename.toLowerCase() + "_" + astname; 
        res = 
         res + "    out.print(\" " + astname + " = \\\"\");\n" +  
               "    List " + r + " = " + ex + ".get" + astname + "();\n" + 
               "    for (int _j = 0; _j < " + r + ".size(); _j++)\n" +
               "    { out.print(\" //@" + e2s + ".\" + " +
                      e2s + ".indexOf(" + r + ".get(_j)));\n" + 
               "    }\n" + 
               "    out.print(\"\\\"\");\n"; 
      }  
    }
    return res +  "    out.println(\" />\");\n  }\n\n"; 
  } 

  public String xsi2SettupModel()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    if (hasStereotype("ERelation"))
    { res = "  eRELATIONS.addAll(" + es + ");\n"; } 
    else 
    { res = "  eELEMENTS.addAll(" + es + ");\n"; } 

    return res +  "\n"; 
  } 

  public String xsi2SaveModel()
  { if (isAbstract() || isInterface() || hasStereotype("external") ||
        hasStereotype("auxiliary") || hasStereotype("externalApp")) 
    { return ""; } 

    String res = "";
    String ename = getName();
    String es = ename.toLowerCase() + "s";
    String ecorekind = ""; 
    if (hasStereotype("ERelation"))
    { ecorekind = "ERelation"; } 
    else 
    { ecorekind = "Eelement"; } 

    String ex = ename.toLowerCase() + "x_";
    res = "    for (int _i = 0; _i < " + es + ".size(); _i++)\n" +
          "    { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" + 
          "       out.print(\"<" + ecorekind  + " xsi:type=\\\"KCL:" + ename + "\\\"\");\n";

    Vector allAttributes = new Vector(); 
    allAttributes.addAll(attributes); 
    allAttributes.addAll(allInheritedAttributes()); 

    for (int j = 0; j < allAttributes.size(); j++)
    { Attribute att = (Attribute) allAttributes.get(j);
      String attname = att.getName();
      res = res + "    out.print(\" " + attname + "=\\\"\" + " + ex + ".get" + attname + "() + \"\\\" \");\n";
    }

    Vector allAssociations = new Vector(); 
    allAssociations.addAll(associations); 
    allAssociations.addAll(allInheritedAssociations()); 

    for (int j = 0; j < allAssociations.size(); j++)
    { Association ast = (Association) allAssociations.get(j);
      if (ast.isQualified()) { continue; } 

      String astname = ast.getRole2();
      Entity ent2 = ast.getEntity2(); 
      String e2name = ent2.getName(); 
      String e2s = e2name.toLowerCase() + "s"; 
      String ecore = ""; 
      String elist = ""; 
      if (ent2.hasStereotype("ERelation"))
      { ecore = "ERelation"; 
        elist = "eRELATIONS"; 
      } 
      else 
      { ecore = "Eelement";
        elist = "eELEMENTS";
      } 

      String e2x = e2name.toLowerCase() + "x_"; 
      if (ast.getCard2() == ONE)
      { res = res + "    out.print(\" " + astname + "=\\\"\");\n" + 
                    "    out.print(\"//@" + ecore + ".\" + " + 
                         elist + ".indexOf(((" + ename + ") " + es + ".get(_i)).get" + 
                  astname + "()));\n" + 
                    "    out.print(\"\\\"\");\n";
      }
      else      
      { String r = ename.toLowerCase() + "_" + astname; 
        res = 
         res + "    out.print(\" " + astname + " = \\\"\");\n" +  
               "    List " + r + " = " + ex + ".get" + 
                  astname + "();\n" + 
               "    for (int _j = 0; _j < " + r + ".size(); _j++)\n" +
               "    { out.print(\" //@" + ecore + ".\" + " +
                      elist + ".indexOf(" + r + ".get(_j)));\n" + 
               "    }\n" + 
               "    out.print(\"\\\"\");\n"; 
      }  
    }
    return res +  "    out.println(\" />\");\n  }\n\n"; 
  } 

  public Vector removeMyUses(Vector euses)
  { Vector removals = new Vector(); 
    Vector res = new Vector(); 

    for (int k = 0; k < euses.size(); k++) 
    { Object u = euses.get(k);
      if (getName().equals(u + ""))
      { removals.add(u); } 
    } 
    euses.removeAll(removals); 
    
    if (superclass != null)
    { res = superclass.removeMyUses(euses); }  
    else 
    { res.addAll(euses); } 

    return res; 
  } 

  public String checkCompletenessOp()
  { String res = ""; 
    Vector keys = getUniqueAttributes();
    if (keys.size() == 0)
    { return res; } 
    Attribute pk = (Attribute) keys.get(0); // do for all of these
    String pkname = pk + ""; 
    String ename = getName(); 
    String es = ename.toLowerCase() + "s"; 
    String ex = ename.toLowerCase() + "_x"; 
    String eobj = ename.toLowerCase() + "_obj"; 
    String mapname = ename.toLowerCase() + pkname + "index"; 
    String accessid = ex + ".get" + pkname + "()"; 

    res = 
      "  for (int _i = 0; _i < " + es + ".size(); _i++)\n" +  
      "  { " + ename + " " + ex + " = (" + ename + ") " + es + ".get(_i);\n" + 
      "    " + ename + " " + eobj + " = (" + ename + ") " + mapname + ".get(" + accessid + ");\n" +  
      "    if (" + eobj + " == " + ex + ") { }\n" +  
      "    else if (" + eobj + " == null)\n" +  
      "    { " + mapname + ".put(" + accessid + "," + ex + "); }\n" +  
      "    else\n" +  
      "    { System.out.println(\"Error: multiple objects with " + pkname + " = \" + " + accessid + "); }\n" + 
      "  }\n"; 
    return res; 
  } 

  public BParallelStatement bDeleteCode(BExpression subs)
  { // for all associations with source this entity, domain remove subs
    // for all associations with range this entity, range remove subs
    // remove subs from all superclasses
    BParallelStatement res; 
    String nme = getName(); 
    String ename = nme.toLowerCase() + "s"; 

    BExpression bes = new BBasicExpression(ename);
    BStatement assgn = new BAssignStatement(bes,
                       new BBinaryExpression("-",bes,subs)); 
    assgn.setWriteFrame(ename); 

    if (superclass != null)
    { res = superclass.bDeleteCode(subs); } 
    else 
    { res = new BParallelStatement(); } 
    res.addStatement(assgn); 

    for (int i = 0; i < associations.size(); i++) 
    { Association ast = (Association) associations.get(i); 
      BExpression bast = new BBasicExpression(ast.getRole2()); 
      BExpression dres = new BBinaryExpression("<<|",subs,bast); 
      BAssignStatement restr = 
        new BAssignStatement(bast,dres); 
      restr.setWriteFrame(ast.getRole2()); 
      res.addStatement(restr); 
    } 
    return res;   
  } 

public BehaviouralFeature designKillOp(Vector assocs)
{ String ename = getName();
  String elower = ename.toLowerCase();
  Type etype = new Type(this);
  Attribute p = new Attribute(elower + "_x", etype, ModelElement.INTERNAL);
  p.setElementType(etype);
  BasicExpression ex = new BasicExpression(p);
  ex.setType(etype); 
  ex.setElementType(etype); 
  BehaviouralFeature bf = new BehaviouralFeature("kill" + ename);
  bf.setStatic(true);
  bf.setQuery(false);
  bf.addParameter(p);
  bf.setOwner(this);
  SequenceStatement ss = new SequenceStatement();
  // remove p from E_instances
  BasicExpression e_instances = new BasicExpression(elower + "_instances", 0);
  Type esettype = new Type("Set", null);
  e_instances.setType(esettype);
  esettype.setElementType(etype);
  e_instances.setElementType(etype);
  BinaryExpression sube = new BinaryExpression("->excluding", e_instances, ex);
  AssignStatement remex = new AssignStatement(e_instances, sube);
  ss.addStatement(remex);

  for (int ii = 0; ii < assocs.size(); ii++)
  { Association ast = (Association) assocs.get(ii);
    if (this == ast.getEntity2())
    { Statement delcode = ast.delete2Op(this);
      ss.addStatement(delcode);
    }
    if (this == ast.getEntity1() && ast.getRole1() != null && ast.getRole1().length() > 0)
    { Statement delcode = ast.delete1Op(this);
      ss.addStatement(delcode);
    }
    else if (this == ast.getEntity1() && ast.isAggregation() && ast.getCard1() == ZEROONE)
    { Statement delagg = ast.deleteAggregationOp(this,ex);
      ss.addStatement(delagg);
    }
  }

  if (superclass != null)
  { String supname = superclass.getName();
    BasicExpression sup = new BasicExpression(superclass);
    Type suptype = new Type(superclass);
    BasicExpression exsup = new BasicExpression("super", 0);
    exsup.setObjectRef(ex);
    exsup.setType(suptype);
    exsup.umlkind = Expression.VARIABLE;
    exsup.setElementType(suptype);

    BasicExpression killsup = new BasicExpression("kill" + supname, 0);
    killsup.setObjectRef(sup);
    killsup.addParameter(exsup);
    killsup.umlkind = Expression.UPDATEOP;
    killsup.setIsEvent();
    InvocationStatement killbx = new InvocationStatement(killsup);
    ss.addStatement(killbx);

    BasicExpression killsup2 = new BasicExpression("kill" + supname, 0);
    killsup2.setObjectRef(sup);
    BinaryExpression castex = new BinaryExpression("->oclAsType", ex, sup); 
    castex.setType(suptype); 
    castex.setElementType(suptype); 
    killsup2.addParameter(castex);
    killsup2.umlkind = Expression.UPDATEOP;
    killsup2.setIsEvent();
    InvocationStatement killbx2 = new InvocationStatement(killsup2);
    ss.addStatement(killbx2);
  }

  if (isAbstract()) { } 
  else 
  { BasicExpression freeex = new BasicExpression("free", 0);
    freeex.addParameter(ex);
    freeex.setIsEvent(); 
    freeex.umlkind = Expression.UPDATEOP;
    InvocationStatement destroyex = new InvocationStatement(freeex);
    ss.addStatement(destroyex); 
  } 
  bf.setActivity(ss);
  return bf;
}

public BehaviouralFeature designAbstractKillOp()
{ String ename = getName();
  String elower = ename.toLowerCase();
  Type etype = new Type(this);
  Attribute p = new Attribute(elower + "_x", etype, ModelElement.INTERNAL);
  p.setElementType(etype);
  BasicExpression ex = new BasicExpression(p);
  BehaviouralFeature bf = new BehaviouralFeature("killAbstract" + ename);
  bf.setStatic(true);
  bf.setQuery(false);
  bf.addParameter(p);
  bf.setOwner(this);
  SequenceStatement ss = new SequenceStatement();
  // remove p from E_instances
  BasicExpression e_instances = new BasicExpression(elower + "_instances", 0);
  Type esettype = new Type("Set", null);
  e_instances.setType(esettype);
  esettype.setElementType(etype);
  e_instances.setElementType(etype);
  SequenceStatement skip = new SequenceStatement();
  Statement cnd0 = skip;
  Vector leaves = getActualLeafSubclasses();
  for (int ii = 0; ii < leaves.size(); ii++)
  { Entity sub = (Entity) leaves.get(ii);
    BasicExpression subinsts = new BasicExpression(sub);
    BinaryExpression tst = new BinaryExpression(":", ex, subinsts); 
    String subname = sub.getName();
    Type subtype = new Type(sub);
    BasicExpression killsub = new BasicExpression("kill" + subname, 0);
    BinaryExpression cst = new BinaryExpression("->oclAsType", ex, subinsts);
    killsub.addParameter(cst);
    killsub.umlkind = Expression.UPDATEOP;
    killsub.setIsEvent();
    InvocationStatement killbx = new InvocationStatement(killsub);

    ConditionalStatement cnd = new ConditionalStatement(tst, killbx, cnd0);
    cnd0 = cnd;
  }
  bf.setActivity(cnd0);
  return bf;
}


  public Vector testCases()
  { Vector res = new Vector(); 
    String nme = getName(); 
    String x = nme.toLowerCase() + "$x"; 
    res.add(x + " : " + nme); 
    
    Vector allattributes = allDefinedAttributes(); 

    for (int i = 0; i < allattributes.size(); i++) 
    { Vector newres = new Vector(); 
      Attribute att = (Attribute) allattributes.get(i); 
      Vector testassignments = att.testCases(x); 
      for (int j = 0; j < res.size(); j++) 
      { String tst = (String) res.get(j); 
        for (int k = 0; k < testassignments.size(); k++) 
        { String kstr = (String) testassignments.get(k); 
          if (kstr.length() > 0) 
          { String newtst = tst + "\n" + kstr; 
            newres.add(newtst); 
          } 
        } 
      } 
      res.clear(); 
      res.addAll(newres); 
    } 
    return res; 
  } 
}
