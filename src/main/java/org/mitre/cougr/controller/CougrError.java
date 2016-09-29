/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.mitre.cougr.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME , property = "class")
public class CougrError {
    private static Logger LOG = LoggerFactory.getLogger(CougrError.class);
    private String message;
    private Throwable throwable;
    private String type;

    public CougrError(Throwable t) {
        this(t.getMessage(), t);
    }

    public CougrError(String message, Throwable throwable) {
        this.message = message;
        this.throwable = throwable;
        this.type = this.throwable.getClass().getCanonicalName();
        LOG.debug(message, throwable);
    }

    public String getMessage() {
        return this.message;
    }

    @JsonIgnore
    public Throwable getThrowable() {
        return this.throwable;
    }

    public String getType() {
        return this.type;
    }
}
