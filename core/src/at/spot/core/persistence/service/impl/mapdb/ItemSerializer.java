package at.spot.core.persistence.service.impl.mapdb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import com.google.gson.Gson;

import at.spot.core.data.model.Item;

public class ItemSerializer<A extends Item> extends GroupSerializerObjectArray<A> {

	@Override
	public A deserialize(DataInput2 in, int available) throws IOException {
		Object value = null;

		try {
			ObjectInputStream in2 = new ObjectInputStream(new DataInput2.DataInputToStream(in));
			value = in2.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}

		return (A) value;
	}

	@Override
	public void serialize(DataOutput2 out, A value) throws IOException {
		Gson gson = new Gson();
		String json = gson.toJson(value);

		ObjectOutputStream out2 = new ObjectOutputStream((OutputStream) out);
		out2.writeObject(json);
		out2.flush();
	}

}
