package com.facebook.presto.sql.planner;

import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.sql.analyzer.Type;
import com.facebook.presto.sql.tree.Input;
import com.facebook.presto.tuple.TupleReadable;
import io.airlift.slice.Slices;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TupleInputResolver
        implements InputResolver
{
    private TupleReadable[] inputs;

    private RecordCursor cursor;
    private Map<Input, Type> inputTypes;

    public void setInputs(TupleReadable[] inputs)
    {
        checkState(cursor == null, "%s already has a cursor set", getClass().getName());
        this.inputs = checkNotNull(inputs, "inputs is null");
    }

    public void setCursor(RecordCursor cursor, Map<Input, Type> tupleInfo)
    {
        checkState(inputs == null, "%s already has inputs set", getClass().getName());
        this.cursor = checkNotNull(cursor, "cursor is null");
        this.inputTypes = checkNotNull(tupleInfo, "tupleInfo is null");
    }

    @Override
    public Object getValue(Input input)
    {
        int channel = input.getChannel();
        if (inputs != null) {
            TupleReadable tuple = inputs[channel];
            int field = input.getField();

            if (tuple.isNull(field)) {
                return null;
            }

            switch (tuple.getTupleInfo().getTypes().get(field)) {
                case BOOLEAN:
                    return tuple.getBoolean(field);
                case FIXED_INT_64:
                    return tuple.getLong(field);
                case DOUBLE:
                    return tuple.getDouble(field);
                case VARIABLE_BINARY:
                    return tuple.getSlice(field);
                default:
                    throw new UnsupportedOperationException("not yet implemented");
            }
        } else if (cursor != null) {
            checkArgument(input.getField() == 0, "Field for cursor must be 0 but is %s", input.getField());

            if (cursor.isNull(channel)) {
                return null;
            }

            switch (inputTypes.get(input)) {
                case BOOLEAN:
                    return cursor.getBoolean(channel);
                case LONG:
                    return cursor.getLong(channel);
                case DOUBLE:
                    return cursor.getDouble(channel);
                case STRING:
                    return Slices.wrappedBuffer(cursor.getString(channel));
                default:
                    throw new UnsupportedOperationException("not yet implemented");
            }
        }
        throw new UnsupportedOperationException("Inputs or cursor myst be set");
    }
}