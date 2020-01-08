package com.github.fge.jjschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.processing.RawProcessor;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.CanonicalSchemaTree;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.core.tree.key.SchemaKey;
import com.github.reinert.jjschema.v1.JsonSchemaFactory;
import com.github.reinert.jjschema.v1.JsonSchemaV4Factory;

public final class JJSchemaProcessor
    extends RawProcessor<Class<?>, SchemaTree>
{
    private static final JsonSchemaFactory FACTORY = new JsonSchemaV4Factory();

    static {
        FACTORY.setAutoPutDollarSchema(true);
    }

    public JJSchemaProcessor()
    {
        super("class", "schema");
    }

    @Override
    protected SchemaTree rawProcess(final ProcessingReport report,
        final Class<?> input)
        throws ProcessingException
    {
        final ProcessingMessage message = newMessage(input);
        report.debug(message.setMessage("processing"));
        final JsonNode node = FACTORY.createSchema(input);
        return new CanonicalSchemaTree(SchemaKey.anonymousKey(), node);
    }
}
