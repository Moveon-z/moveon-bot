package com.moveon.infra.config;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Hibernate 自定义类型：将 Java float[] 与 PostgreSQL vector 类型互转
 */
public class VectorType implements UserType<float[]> {

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        Object obj = rs.getObject(position);
        if (obj == null) {
            return null;
        }
        if (obj instanceof PGvector pgVector) {
            return pgVector.toArray();
        }
        if (obj instanceof String str) {
            return new PGvector(str).toArray();
        }
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, float[] value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value != null) {
            ps.setObject(index, new PGvector(value));
        } else {
            ps.setNull(index, Types.OTHER, "vector");
        }
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value != null ? Arrays.copyOf(value, value.length) : null;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return (float[]) cached;
    }
}
