package com.ytt.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytt.entity.ExamBank;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * exam_bank.subjects（JSON 数组）与 List&lt;ExamBank.Subject&gt; 的类型处理器。
 *
 * <p>MyBatis-Plus 3.5.5 自带的 JacksonTypeHandler 只持有字段的原始类型 List.class，
 * 反序列化时会得到 List&lt;LinkedHashMap&gt;，强转为 Subject 抛 ClassCastException，
 * 因此这里通过 TypeReference 保留完整泛型参数。</p>
 */
public class SubjectListTypeHandler extends BaseTypeHandler<List<ExamBank.Subject>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<ExamBank.Subject>> TYPE =
            new TypeReference<List<ExamBank.Subject>>() {
            };

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ExamBank.Subject> parameter,
                                    JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("科目定义 JSON 序列化失败", e);
        }
    }

    @Override
    public List<ExamBank.Subject> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<ExamBank.Subject> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<ExamBank.Subject> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<ExamBank.Subject> parse(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            throw new SQLException("科目定义 JSON 解析失败: " + json, e);
        }
    }
}
