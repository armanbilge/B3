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
import java.util.HashSet;
import java.util.Set;

/**
 * @author Arman Bilge
 */
public class Serializer<T extends Serializable> {

    final T object;
    final File file;
    final File backup;
    final Kryo kryo;
    final Set<Resumable> resumables = new HashSet<>();

    public interface Resumable {
        void resume();
    }

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
        kryo = new Kryo() {
            @Override
            public <T> T readObject(Input input, Class<T> type) {
                final T object = super.readObject(input, type);
                if (object instanceof Resumable)
                    resumables.add((Resumable) object);
                return object;
            }
            @Override
            public <T> T readObject(Input input, Class<T> type, com.esotericsoftware.kryo.Serializer serializer) {
                final T object = super.readObject(input, type, serializer);
                if (object instanceof Resumable)
                    resumables.add((Resumable) object);
                return object;
            }
        };
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
        resumables.clear();
        final Input in;
        try {
            in = new Input(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new SerializationException(e);
        }
        final T object = kryo.readObject(in, objectClass);
        in.close();
        for (final Resumable r : resumables) r.resume();
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
