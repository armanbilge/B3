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
import beast.evolution.tree.TreeLogger;
import beast.inference.loggers.Logger;
import beast.inference.loggers.MCLogger;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arman Bilge
 */
public class Serializer<T extends Identifiable> {

    final T object;
    final File file;
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

        final com.esotericsoftware.kryo.Serializer<MCLogger> mcLoggerSerializer = kryo.getSerializer(MCLogger.class);
        kryo.register(MCLogger.class, new com.esotericsoftware.kryo.Serializer<MCLogger>() {
            @Override
            public void write(Kryo kryo, Output output, MCLogger mcLogger) {
                mcLoggerSerializer.write(kryo, output, mcLogger);
            }

            @Override
            public MCLogger read(Kryo kryo, Input input, Class<MCLogger> aClass) {
                final MCLogger mcLogger = mcLoggerSerializer.read(kryo, input, aClass);
                loggers.add(mcLogger);
                return mcLogger;
            }
        });

        final com.esotericsoftware.kryo.Serializer<TreeLogger> treeLoggerSerializer = kryo.getSerializer(TreeLogger.class);
        kryo.register(TreeLogger.class, new com.esotericsoftware.kryo.Serializer<TreeLogger>() {
            @Override
            public void write(Kryo kryo, Output output, TreeLogger treeLogger) {
                treeLoggerSerializer.write(kryo, output, treeLogger);
            }
            @Override
            public TreeLogger read(Kryo kryo, Input input, Class<TreeLogger> aClass) {
                final TreeLogger treeLogger = treeLoggerSerializer.read(kryo, input, aClass);
                loggers.add(treeLogger);
                return treeLogger;
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
        beagleTreeLikelihoods.clear();
        final Input in = new Input(new FileInputStream(file));
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

}
