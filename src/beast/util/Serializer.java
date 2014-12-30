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

import beagle.BeagleInfo;
import beast.beagle.treelikelihood.BeagleTreeLikelihood;
import beast.inference.loggers.Logger;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Arman Bilge
 */
public class Serializer<T extends Serializable> {

    final T object;
    final File file;
    final File backup;
    final Kryo kryo;
    final List<Logger> loggers;
    final List<BeagleTreeLikelihood> beagleTreeLikelihoods;

    public static class SyntheticFieldSerializer<T> extends FieldSerializer<T> {
        {
            setIgnoreSyntheticFields(false);
        }
        public SyntheticFieldSerializer(Kryo kryo, Class type) {
            super(kryo, type);
        }
        public SyntheticFieldSerializer(Kryo kryo, Class type, Class[] generics) {
            super(kryo, type, generics);
        }
    }

    {
        kryo = new Kryo();
        kryo.setDefaultSerializer(SyntheticFieldSerializer.class);

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

        loggers = new ArrayList<>();

        kryo.register(Logger.class, new com.esotericsoftware.kryo.Serializer<Logger>() {
            final Map<Class<? extends Logger>, com.esotericsoftware.kryo.Serializer<Logger>> serializers =
                    new HashMap<>();
            @Override
            public void write(Kryo kryo, Output output, Logger logger) {
                getSerializer(logger.getClass()).write(kryo, output, logger);
            }
            @Override
            public Logger read(Kryo kryo, Input input, Class<Logger> aClass) {
                final Logger logger = getSerializer(aClass).read(kryo, input, aClass);
                loggers.add(logger);
                return logger;
            }
            com.esotericsoftware.kryo.Serializer<Logger> getSerializer(Class<? extends Logger> aClass) {
                if (!serializers.containsKey(aClass))
                    serializers.put(aClass, new SyntheticFieldSerializer<Logger>(kryo, aClass));
                return serializers.get(aClass);
            }
        });

        beagleTreeLikelihoods = new ArrayList<>();
        final com.esotericsoftware.kryo.Serializer<BeagleTreeLikelihood> beagleTreeLikelihoodSerializer =
                kryo.getSerializer(BeagleTreeLikelihood.class);
        kryo.register(BeagleTreeLikelihood.class, new com.esotericsoftware.kryo.Serializer<BeagleTreeLikelihood>() {
            @Override
            public void write(Kryo kryo, Output output, BeagleTreeLikelihood beagleTreeLikelihood) {
                beagleTreeLikelihoodSerializer.write(kryo, output, beagleTreeLikelihood);
            }

            @Override
            public BeagleTreeLikelihood read(Kryo kryo, Input input, Class<BeagleTreeLikelihood> aClass) {
                final BeagleTreeLikelihood beagleTreeLikelihood = beagleTreeLikelihoodSerializer.read(kryo, input, aClass);
                beagleTreeLikelihoods.add(beagleTreeLikelihood);
                return beagleTreeLikelihood;
            }
        });
    }

    public Serializer(final File file, final T object) throws SerializationException {
        this.file = file;
        backup = createBackupFile();
        this.object = object;
    }

    public Serializer(final File file, final Class<? extends T> objectClass) throws SerializationException {
        this.file = file;
        backup = createBackupFile();
        object = this.deserialize(objectClass);
    }

    public void serialize() throws SerializationException {
        file.renameTo(backup);
        final Output out;
        try {
            out = new Output(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new SerializationException(e);
        }
        kryo.writeObject(out, object);
        out.close();
        backup.delete();
    }

    private File createBackupFile() throws SerializationException {
        try {
            return new File(file.getCanonicalPath() + ".bak");
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    private T deserialize(final Class<? extends T> objectClass) throws SerializationException {
        beagleTreeLikelihoods.clear();
        final Input in;
        try {
            in = new Input(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new SerializationException(e);
        }
        final T object = kryo.readObject(in, objectClass);
        in.close();
        if (loggers.size() > 0) {
            for (final Logger l : loggers)
                l.resumeLog();
        }
        if (beagleTreeLikelihoods.size() > 0) {
            BeagleInfo.printVersionInformation();
            for (final BeagleTreeLikelihood btl : beagleTreeLikelihoods)
                btl.reloadBeagle();
        }
        return object;
    }

    public T getObject() {
        return object;
    }

    public static class SerializationException extends Exception {
        public SerializationException(final Exception ex) {
            super("Problem with automatic serialization.", ex);
        }
    }

}
