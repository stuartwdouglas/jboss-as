package org.jboss.as.ejb3.remote.protocol;

import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.SunReflectiveCreator;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public class MarshallingSupport {

    private MarshallingSupport() {}


    /**
     * Creates and returns a {@link org.jboss.marshalling.Marshaller} which is ready to be used for marshalling. The {@link org.jboss.marshalling.Marshaller#start(org.jboss.marshalling.ByteOutput)}
     * will be invoked by this method, to use the passed {@link java.io.DataOutput dataOutput}, before returning the marshaller.
     *
     * @param marshallerFactory The marshaller factory
     * @param dataOutput        The {@link java.io.DataOutput} to which the data will be marshalled
     * @return
     * @throws IOException
     */
    public static org.jboss.marshalling.Marshaller prepareForMarshalling(final org.jboss.marshalling.MarshallerFactory marshallerFactory, final DataOutput dataOutput) throws IOException {
        final org.jboss.marshalling.Marshaller marshaller = getMarshaller(marshallerFactory);
        final OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                final int byteToWrite = b & 0xff;
                dataOutput.write(byteToWrite);
            }

            @Override
            public void write(final byte[] b) throws IOException {
                dataOutput.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                dataOutput.write(b, off, len);
            }
        };
        final ByteOutput byteOutput = Marshalling.createByteOutput(outputStream);
        // start the marshaller
        marshaller.start(byteOutput);

        return marshaller;
    }

    /**
     * Creates and returns a {@link org.jboss.marshalling.Marshaller}
     *
     * @param marshallerFactory The marshaller factory
     * @return
     * @throws IOException
     */
    private static org.jboss.marshalling.Marshaller getMarshaller(final org.jboss.marshalling.MarshallerFactory marshallerFactory) throws IOException {
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setClassTable(ProtocolV1ClassTable.INSTANCE);
        marshallingConfiguration.setObjectTable(ProtocolV1ObjectTable.INSTANCE);
        marshallingConfiguration.setVersion(2);
        marshallingConfiguration.setSerializedCreator(new SunReflectiveCreator());
        return marshallerFactory.createMarshaller(marshallingConfiguration);
    }

    /**
     * Creates and returns a {@link org.jboss.marshalling.Unmarshaller} which is ready to be used for unmarshalling. The {@link org.jboss.marshalling.Unmarshaller#start(org.jboss.marshalling.ByteInput)}
     * will be invoked by this method, to use the passed {@link java.io.DataInput dataInput}, before returning the unmarshaller.
     *
     * @param marshallerFactory The marshaller factory
     * @param classResolver     The {@link ClassResolver} which will be used during unmarshalling
     * @param dataInput         The data input from which to unmarshall
     * @return
     * @throws IOException
     */
    public static Unmarshaller prepareForUnMarshalling(final MarshallerFactory marshallerFactory, final ClassResolver classResolver, final DataInputStream dataInput) throws IOException {
        final Unmarshaller unmarshaller = getUnMarshaller(marshallerFactory, classResolver);
        final InputStream is = new InputStream() {
            @Override
            public int read() throws IOException {
                try {

                    final int b = dataInput.readByte();
                    return b & 0xff;
                } catch (EOFException eof) {
                    return -1;
                }
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                return dataInput.read(b, off, len);
            }

            @Override
            public int read(final byte[] b) throws IOException {
                return dataInput.read(b);
            }
        };
        final ByteInput byteInput = Marshalling.createByteInput(is);
        // start the unmarshaller
        unmarshaller.start(byteInput);

        return unmarshaller;
    }

    /**
     * Creates and returns a {@link Unmarshaller}
     *
     * @param marshallerFactory The marshaller factory
     * @return
     * @throws IOException
     */
    private static Unmarshaller getUnMarshaller(final MarshallerFactory marshallerFactory, final ClassResolver classResolver) throws IOException {
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setVersion(2);
        marshallingConfiguration.setClassTable(ProtocolV1ClassTable.INSTANCE);
        marshallingConfiguration.setObjectTable(ProtocolV1ObjectTable.INSTANCE);
        marshallingConfiguration.setClassResolver(classResolver);
        marshallingConfiguration.setSerializedCreator(new SunReflectiveCreator());
        return marshallerFactory.createUnmarshaller(marshallingConfiguration);
    }


    public static Map<String, Object> readAttachments(final ObjectInput input) throws IOException, ClassNotFoundException {
        final int numAttachments = input.readByte();
        if (numAttachments == 0) {
            return new HashMap<String, Object>();
        }
        final Map<String, Object> attachments = new HashMap<String, Object>(numAttachments);
        for (int i = 0; i < numAttachments; i++) {
            // read the key
            final String key = (String) input.readObject();
            // read the attachment value
            final Object val = input.readObject();
            attachments.put(key, val);
        }
        return attachments;
    }

    public static void writeAttachments(final ObjectOutput output, final Map<String, Object> attachments) throws IOException {
        if (attachments == null) {
            output.writeByte(0);
            return;
        }
        // write the attachment count
        PackedInteger.writePackedInteger(output, attachments.size());
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            output.writeObject(entry.getKey());
            output.writeObject(entry.getValue());
        }
    }

}
