/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2011 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package transformation.sas.overcover.planninggraph;

import transformation.sas.overcover.entities.StandardAtom;
import java.util.LinkedHashSet;

/**
 *
 * @author Filip Dvořák
 */
public class AtomLayer {

    public LinkedHashSet<StandardAtom> facts;

    public AtomLayer() {
        facts = new LinkedHashSet<>();
    }
}
