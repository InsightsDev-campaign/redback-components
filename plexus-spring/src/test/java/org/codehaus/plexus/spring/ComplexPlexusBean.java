package org.codehaus.plexus.spring;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;
import java.util.Map;


/**
 * A typical plexus component implementation
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class ComplexPlexusBean
{
    /**
     * @plexus.requirement role="org.codehaus.plexus.spring.PlexusBean"
     */
    private Map plexusBeans;

    /**
     * @plexus.requirement role="org.codehaus.plexus.spring.PlexusBean"
     */
    private List beansList;

    /**
     * @see org.codehaus.plexus.spring.PlexusBean#toString()
     */
    public String toString()
    {
        return plexusBeans.size() + " components for role org.codehaus.plexus.spring.PlexusBean";
    }

    public Map getBeans()
    {
        return plexusBeans;
    }

    public List getBeansList()
    {
        return beansList;
    }
}