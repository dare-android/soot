package soot.dava.internal.javaRep;

import soot.*;
import java.util.*;
import soot.grimp.*;
import soot.grimp.internal.*;

public class DInterfaceInvokeExpr extends GInterfaceInvokeExpr
{
    public DInterfaceInvokeExpr( Value base, SootMethod method, java.util.List args) 
    {
	super( base, method, args);
    }

    public String toBriefString()
    {
	return toString();
    }

    public String toString()
    {
	return super.toBriefString();
    }

    public Object clone() 
    {
        ArrayList clonedArgs = new ArrayList( getArgCount());

        for(int i = 0; i < getArgCount(); i++) 
            clonedArgs.add(i, Grimp.cloneIfNecessary(getArg(i)));
        
        return new DInterfaceInvokeExpr( getBase(), getMethod(), clonedArgs);
    }
}