package com.github.fge.jsonschema2pojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.InvalidSchemaException;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.processing.RawProcessor;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.core.util.ValueHolder;
import com.google.common.io.Files;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public final class JsonSchema2SourceCode
    extends RawProcessor<SchemaTree, String>
{
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String CLASSNAME = "Whatever";
    private static final String PKGNAME = "com.github.fge.compiled";

    private final Processor<ValueHolder<SchemaTree>, ValueHolder<SchemaTree>>
        processor = new DraftV3OnlySyntaxProcessor();

    public JsonSchema2SourceCode()
    {
        super("schema", "sourceCode");
    }

    @Override
    protected String rawProcess(final ProcessingReport report,
        final SchemaTree input)
        throws ProcessingException
    {
        /*
         * First check if the syntax is valid
         */
        final ValueHolder<SchemaTree> holder
            = ValueHolder.hold("schema", input);
        processor.process(report, holder);
        if (!report.isSuccess())
            throw new InvalidSchemaException(new ProcessingMessage()
                .setMessage("invalid schema, cannot continue"));

        final JsonNode schema = input.getBaseNode();
        final JCodeModel model = new JCodeModel();

        /*
         * Customize generated code
         */
        final Annotator annotator = new Jackson2Annotator(GENCFG);
        final SchemaStore store = new SchemaStore();
        final RuleFactory ruleFactory
            = new RuleFactory(GENCFG, annotator, store);

        final SchemaMapper mapper
            = new SchemaMapper(ruleFactory, new SchemaGenerator());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final CodeWriter writer = new SingleStreamCodeWriter(out);

        final File file;
        try {
            file = schemaToFile(schema);
        } catch (IOException e) {
            throw new ProcessingException("cannot create file", e);
        }

        try {
            mapper.generate(model, CLASSNAME, PKGNAME, file.toURI().toURL());
            model.build(writer);
            return out.toString("UTF-8");
        } catch (IOException e) {
            throw new ProcessingException("failed to generate source", e);
        } finally {
            if (!file.delete())
                report.warn(newMessage(input).setMessage("cannot delete file"));
        }
    }

    /*
     * We unfortunately have to do this :(
     */
    private static File schemaToFile(final JsonNode schema)
        throws IOException
    {
        final File ret = File.createTempFile("schema", ".json");
        final byte[] content = schema.toString().getBytes(UTF8);
        Files.write(content, ret);
        return ret;
    }

    private static final GenerationConfig GENCFG
        = new DefaultGenerationConfig()
    {
        @Override
        public boolean isUsePrimitives()
        {
            return true;
        }

        @Override
        public boolean isIncludeHashcodeAndEquals()
        {
            return false;
        }

        @Override
        public boolean isIncludeToString()
        {
            return false;
        }

        @Override
        public boolean isIncludeJsr303Annotations()
        {
            return false;
        }
    };
}

