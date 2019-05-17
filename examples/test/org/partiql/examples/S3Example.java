package org.partiql.examples;

import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.eval.Bindings;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.Expression;

import java.io.IOException;

public class S3Example {
    public static void main(String... args) {
        /*
            S3 bucket contain JSON lines formatted data, example:
            {"id": "1", "name": "person_1", "age": 32, "address": "555 1st street, Seattle", "tags": []}
            {"id": "2", "name": "person_2", "age": 24}
            {"id": "3", "name": "person_3", "age": 25, "address": {"number": 555, "street": "1st street", "city": "Seattle"}, "tags": ["premium_user"]}
         */

        final String bucket_name = "";
        final String key_name = "";
        final String region = "";

        final AmazonS3 s3 = AmazonS3Client.builder().withRegion(region).build();

        // Initializes the ion system used by PartiQL
        final IonSystem ion = IonSystemBuilder.standard().build();

        // CompilerPipeline is the main entry point for the PartiQL lib giving you access to the compiler
        // and value factories
        final CompilerPipeline pipeline = CompilerPipeline.standard(ion);

        // Compiles the query, the resulting expression can be re-used to query multiple data sets
        final Expression selectAndFilter = pipeline.compile(
                "SELECT doc.name, doc.address FROM myS3Document doc WHERE doc.age < 30");

        try (
                final S3Object s3Object = s3.getObject(bucket_name, key_name);
                final S3ObjectInputStream s3InputStream = s3Object.getObjectContent();

                // We are using ion-java to parse the JSON data as PartiQL comes with an embedded value factory for
                // Ion data and Ion being a superset of JSON any JSON data is also Ion data
                // http://amzn.github.io/ion-docs/
                // https://github.com/amzn/ion-java
                final IonReader ionReader = IonReaderBuilder.standard().build(s3InputStream);

                // We are using ion-java again to dump the PartiQL query result as JSON
                final IonWriter resultWriter = IonTextWriterBuilder.json().build((Appendable) System.out);
        ) {
            // Parses all data from the S3 bucket into the Ion DOM
            final IonDatagram values = ion.getLoader().load(ionReader);

            // Evaluation session encapsulates all information to evaluate a PartiQL expression, including the
            // global bindings
            final EvaluationSession session = EvaluationSession.builder()
                    // We implement the Bindings interface using a lambda. Bindings are used to map names into values,
                    // in this case we are binding the data from the S3 bucket into the "myS3Document" name
                    .globals(
                            Bindings.lazyBindingsBuilder()
                                    .addBinding("myS3Document", () -> pipeline.getValueFactory().newFromIonValue(values))
                                    .build()
                    )
                    .build();

            // Executes the query in the session that's encapsulating the JSON data
            final ExprValue selectAndFilterResult = selectAndFilter.eval(session);

            // Uses ion-java to dump the result as JSON. It's possible to build your own writer and dump the ExprValue
            // as any format you want.
            selectAndFilterResult.getIonValue().writeTo(resultWriter);
            // result as JSON bellow
            // [{"name":"person_2"},{"name":"person_3","address":{"number":555,"street":"1st street","city":"Seattle"}}]
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
