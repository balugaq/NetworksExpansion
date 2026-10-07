package com.balugaq.netex.api.enums;

import lombok.Getter;

/**
 * @author balugaq
 */
@Getter
public enum AccessMode {
    INPUT(true, false),
    OUTPUT(false, true),
    ALL(true, true);

    final boolean inputAccess, outputAccess;

    AccessMode(boolean inputAccess, boolean outputAccess) {
        this.inputAccess = inputAccess;
        this.outputAccess = outputAccess;
    }
}
