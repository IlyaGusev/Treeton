/*
 *  TokeniserException.java
 *
 *  Copyright (c) 1998-2001, The University of Sheffield.
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 27/06/2000
 *
 *  $Id: TokeniserException.java,v 1.6 2000/11/08 16:34:55 hamish Exp $
 */

package treeton.res.tokeniser;

/**
 * The top level exception for all the exceptions fired by the tokeniser
 */
public class TokeniserException extends Exception {
    public TokeniserException(String text) {
        super(text);
    }

} // class TokeniserException
