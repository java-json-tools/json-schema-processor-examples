package com.github.fge.jsonschema2pojo;


import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public final class UnsupportedVersionException
    extends ProcessingException
{
    private static final long serialVersionUID = -1629731636752469256L;

    public UnsupportedVersionException()
    {
        super("only draft v3 is supported");
    }
}
