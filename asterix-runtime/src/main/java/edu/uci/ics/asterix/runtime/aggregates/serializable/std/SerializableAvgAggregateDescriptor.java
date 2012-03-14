package edu.uci.ics.asterix.runtime.aggregates.serializable.std;

import java.io.DataOutput;
import java.io.IOException;

import edu.uci.ics.asterix.common.config.GlobalConfig;
import edu.uci.ics.asterix.common.functions.FunctionConstants;
import edu.uci.ics.asterix.dataflow.data.nontagged.serde.ADoubleSerializerDeserializer;
import edu.uci.ics.asterix.dataflow.data.nontagged.serde.AFloatSerializerDeserializer;
import edu.uci.ics.asterix.dataflow.data.nontagged.serde.AInt16SerializerDeserializer;
import edu.uci.ics.asterix.dataflow.data.nontagged.serde.AInt32SerializerDeserializer;
import edu.uci.ics.asterix.dataflow.data.nontagged.serde.AInt64SerializerDeserializer;
import edu.uci.ics.asterix.dataflow.data.nontagged.serde.AInt8SerializerDeserializer;
import edu.uci.ics.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import edu.uci.ics.asterix.om.base.ADouble;
import edu.uci.ics.asterix.om.base.AMutableDouble;
import edu.uci.ics.asterix.om.base.ANull;
import edu.uci.ics.asterix.om.types.ATypeTag;
import edu.uci.ics.asterix.om.types.BuiltinType;
import edu.uci.ics.asterix.om.types.EnumDeserializer;
import edu.uci.ics.asterix.runtime.aggregates.base.AbstractSerializableAggregateFunctionDynamicDescriptor;
import edu.uci.ics.hyracks.algebricks.core.algebra.functions.FunctionIdentifier;
import edu.uci.ics.hyracks.algebricks.core.algebra.runtime.base.IEvaluator;
import edu.uci.ics.hyracks.algebricks.core.algebra.runtime.base.IEvaluatorFactory;
import edu.uci.ics.hyracks.algebricks.core.algebra.runtime.base.ISerializableAggregateFunction;
import edu.uci.ics.hyracks.algebricks.core.algebra.runtime.base.ISerializableAggregateFunctionFactory;
import edu.uci.ics.hyracks.algebricks.core.api.exceptions.AlgebricksException;
import edu.uci.ics.hyracks.algebricks.core.api.exceptions.NotImplementedException;
import edu.uci.ics.hyracks.api.dataflow.value.ISerializerDeserializer;
import edu.uci.ics.hyracks.dataflow.common.data.accessors.ArrayBackedValueStorage;
import edu.uci.ics.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class SerializableAvgAggregateDescriptor extends AbstractSerializableAggregateFunctionDynamicDescriptor {

    private static final long serialVersionUID = 1L;
    public final static FunctionIdentifier FID = new FunctionIdentifier(FunctionConstants.ASTERIX_NS, "avg-serial", 1,
            true);

    @Override
    public FunctionIdentifier getIdentifier() {
        return FID;
    }

    @Override
    public ISerializableAggregateFunctionFactory createAggregateFunctionFactory(IEvaluatorFactory[] args)
            throws AlgebricksException {
        final IEvaluatorFactory[] evals = args;

        return new ISerializableAggregateFunctionFactory() {
            private static final long serialVersionUID = 1L;

            public ISerializableAggregateFunction createAggregateFunction() throws AlgebricksException {
                return new ISerializableAggregateFunction() {
                    private ArrayBackedValueStorage inputVal = new ArrayBackedValueStorage();
                    private IEvaluator eval = evals[0].createEvaluator(inputVal);

                    private AMutableDouble aDouble = new AMutableDouble(0);
                    @SuppressWarnings("unchecked")
                    private ISerializerDeserializer<ADouble> doubleSerde = AqlSerializerDeserializerProvider.INSTANCE
                            .getSerializerDeserializer(BuiltinType.ADOUBLE);
                    @SuppressWarnings("unchecked")
                    private ISerializerDeserializer<ANull> nullSerde = AqlSerializerDeserializerProvider.INSTANCE
                            .getSerializerDeserializer(BuiltinType.ANULL);

                    @Override
                    public void init(DataOutput state) throws AlgebricksException {
                        try {
                            state.writeDouble(0.0);
                            state.writeLong(0);
                            state.writeBoolean(false);
                        } catch (IOException e) {
                            throw new AlgebricksException(e);
                        }
                    }

                    @Override
                    public void step(IFrameTupleReference tuple, byte[] state, int start, int len)
                            throws AlgebricksException {
                        inputVal.reset();
                        eval.evaluate(tuple);
                        double sum = BufferSerDeUtil.getDouble(state, start);
                        long count = BufferSerDeUtil.getLong(state, start + 8);
                        boolean metNull = BufferSerDeUtil.getBoolean(state, start + 16);
                        if (inputVal.getLength() > 0) {
                            ++count;
                            ATypeTag typeTag = EnumDeserializer.ATYPETAGDESERIALIZER
                                    .deserialize(inputVal.getBytes()[0]);
                            switch (typeTag) {
                                case INT8: {
                                    byte val = AInt8SerializerDeserializer.getByte(inputVal.getBytes(), 1);
                                    sum += val;
                                    break;
                                }
                                case INT16: {
                                    short val = AInt16SerializerDeserializer.getShort(inputVal.getBytes(), 1);
                                    sum += val;
                                    break;
                                }
                                case INT32: {
                                    int val = AInt32SerializerDeserializer.getInt(inputVal.getBytes(), 1);
                                    sum += val;
                                    break;
                                }
                                case INT64: {
                                    long val = AInt64SerializerDeserializer.getLong(inputVal.getBytes(), 1);
                                    sum += val;
                                    break;
                                }
                                case FLOAT: {
                                    float val = AFloatSerializerDeserializer.getFloat(inputVal.getBytes(), 1);
                                    sum += val;
                                    break;
                                }
                                case DOUBLE: {
                                    double val = ADoubleSerializerDeserializer.getDouble(inputVal.getBytes(), 1);
                                    sum += val;
                                    break;
                                }
                                case NULL: {
                                    metNull = true;
                                    break;
                                }
                                default: {
                                    throw new NotImplementedException("Cannot compute AVG for values of type "
                                            + typeTag);
                                }
                            }
                            inputVal.reset();
                        }
                        BufferSerDeUtil.writeDouble(sum, state, start);
                        BufferSerDeUtil.writeLong(count, state, start + 8);
                        BufferSerDeUtil.writeBoolean(metNull, state, start + 16);
                    }

                    @Override
                    public void finish(byte[] state, int start, int len, DataOutput result) throws AlgebricksException {
                        double sum = BufferSerDeUtil.getDouble(state, start);
                        long count = BufferSerDeUtil.getLong(state, start + 8);
                        boolean metNull = BufferSerDeUtil.getBoolean(state, start + 16);

                        if (count == 0) {
                            GlobalConfig.ASTERIX_LOGGER.fine("AVG aggregate ran over empty input.");
                        } else {
                            try {
                                if (metNull)
                                    nullSerde.serialize(ANull.NULL, result);
                                else {
                                    aDouble.setValue(sum / count);
                                    doubleSerde.serialize(aDouble, result);
                                }
                            } catch (IOException e) {
                                throw new AlgebricksException(e);
                            }
                        }
                    }

                    @Override
                    public void finishPartial(byte[] data, int start, int len, DataOutput partialResult)
                            throws AlgebricksException {
                        try {
                            partialResult.write(data, start, len);
                        } catch (IOException e) {
                            throw new AlgebricksException(e);
                        }
                    }

                };
            }
        };
    }
}