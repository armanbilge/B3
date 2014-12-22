/*
 * Serializer.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.util;

import beagle.BeagleJNIWrapper;
import beast.beagle.treelikelihood.BeagleTreeLikelihood;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

/**
 * @author Arman Bilge
 */
public class Serializer<T extends Identifiable> {

    final T object;
    final File file;
    final Kryo kryo;

    {
        kryo = new Kryo() {
            @Override
            public com.esotericsoftware.kryo.Serializer getDefaultSerializer(Class type) {
                final com.esotericsoftware.kryo.Serializer s = super.getDefaultSerializer(type);
                if (s instanceof FieldSerializer)
                    ((FieldSerializer) s).setIgnoreSyntheticFields(false);
                return s;
            }
        };

        for (final Charset cs : Charset.availableCharsets().values()) {
            kryo.register(cs.getClass(), new com.esotericsoftware.kryo.Serializer<Charset>() {
                @Override
                public void write(Kryo kryo, Output output, Charset charset) {
                    // Nothing to do
                }
                @Override
                public Charset read(Kryo kryo, Input input, Class aClass) {
                    return cs;
                }
            });
        }

        final com.esotericsoftware.kryo.Serializer<BeagleTreeLikelihood> beagleTreeLikelihoodSerializer =
                kryo.getSerializer(BeagleTreeLikelihood.class);
        kryo.register(BeagleTreeLikelihood.class, new com.esotericsoftware.kryo.Serializer<BeagleTreeLikelihood>() {
            @Override
            public void write(Kryo kryo, Output output, BeagleTreeLikelihood beagleTreeLikelihood) {
                beagleTreeLikelihoodSerializer.write(kryo, output, beagleTreeLikelihood);
            }
            @Override
            public BeagleTreeLikelihood read(Kryo kryo, Input input, Class<BeagleTreeLikelihood> aClass) {
                final BeagleTreeLikelihood beagleTreeLikelihood =
                        beagleTreeLikelihoodSerializer.read(kryo, input, aClass);
                if (BeagleJNIWrapper.INSTANCE == null) BeagleJNIWrapper.loadBeagleLibrary();
                return beagleTreeLikelihood;
            }
        });

        final com.esotericsoftware.kryo.Serializer<SerializablePrintWriter> printWriterSerializer =
                kryo.getSerializer(SerializablePrintWriter.class);
        kryo.register(SerializablePrintWriter.class, new com.esotericsoftware.kryo.Serializer<SerializablePrintWriter>() {
            @Override
            public void write(Kryo kryo, Output output, SerializablePrintWriter serializablePrintWriter) {
                printWriterSerializer.write(kryo, output, serializablePrintWriter);
            }
            @Override
            public SerializablePrintWriter read(Kryo kryo, Input input, Class<SerializablePrintWriter> aClass) {
                final SerializablePrintWriter serializablePrintWriter = printWriterSerializer.read(kryo, input, aClass);
                try {
                    return new SerializablePrintWriter(serializablePrintWriter.getFile(), true);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Serializer(final T object) {
        this.object = object;
        String fileName = object.getId();
        if (fileName == null)
            fileName = object.getClass().getSimpleName();
        fileName += ".state";
        file = FileHelpers.getFile(fileName);
    }

    public Serializer(final File file, final Class<? extends T> objectClass) throws FileNotFoundException {
        this.file = file;
        object = this.deserialize(objectClass);
    }

    public void serialize() throws FileNotFoundException {
        final Output out = new Output(new FileOutputStream(file));
        kryo.writeObject(out, object);
        out.close();
    }

    private T deserialize(final Class<? extends T> objectClass) throws FileNotFoundException {
        final Input in = new Input(new FileInputStream(file));
        final T object = kryo.readObject(in, objectClass);
        in.close();
        return object;
    }

    public T getObject() {
        return object;
    }

}
