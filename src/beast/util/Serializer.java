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

/**
 * Created by armanbilge on 11/25/14.
 */
public class Serializer<T extends Identifiable> {

    final T object;
    final File file;
    final Kryo kryo = new Kryo() {
        @Override
        public com.esotericsoftware.kryo.Serializer getDefaultSerializer(Class type) {
            final com.esotericsoftware.kryo.Serializer s = super.getDefaultSerializer(type);
            if (s instanceof FieldSerializer)
                ((FieldSerializer) s).setIgnoreSyntheticFields(false);
            return s;
        }
    };

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
