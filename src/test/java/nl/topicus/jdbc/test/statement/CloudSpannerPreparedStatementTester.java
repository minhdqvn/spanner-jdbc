package nl.topicus.jdbc.test.statement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.api.client.util.Lists;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.Op;
import com.google.cloud.spanner.Value;

import nl.topicus.jdbc.statement.CloudSpannerPreparedStatement;

public class CloudSpannerPreparedStatementTester
{
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testDeleteStatementWithoutWhereClause() throws SQLException
	{
		Mutation deleteMutation = getMutation("DELETE FROM FOO");
		Assert.assertNotNull(deleteMutation);
		Assert.assertEquals(Op.DELETE, deleteMutation.getOperation());
		Assert.assertTrue(deleteMutation.getKeySet().isAll());
		Assert.assertNotNull(deleteMutation);
		Assert.assertEquals(Op.DELETE, deleteMutation.getOperation());
		Assert.assertTrue(deleteMutation.getKeySet().isAll());
	}

	@Test
	public void testDeleteStatementWithWhereClause() throws SQLException
	{
		Mutation deleteMutation = getMutation("DELETE FROM FOO WHERE ID=1");
		Assert.assertNotNull(deleteMutation);
		Assert.assertEquals(Op.DELETE, deleteMutation.getOperation());
		List<Key> keys = Lists.newArrayList(deleteMutation.getKeySet().getKeys());
		Assert.assertEquals(1, keys.size());
		Assert.assertEquals(1l, keys.get(0).getParts().iterator().next());
	}

	@Test
	public void testDeleteStatementWithInWhereClauses() throws SQLException
	{
		thrown.expect(SQLException.class);
		thrown.expectMessage("The DELETE statement does not contain a valid WHERE clause.");
		getMutation("DELETE FROM FOO WHERE ID IN (1,2)");
	}

	@Test()
	public void testDeleteStatementWithInvalidWhereClause() throws SQLException
	{
		thrown.expect(SQLException.class);
		thrown.expectMessage("The DELETE statement does not contain a valid WHERE clause.");
		getMutation("DELETE FROM FOO WHERE ID<2");
	}

	@Test
	public void testUpdateStatementWithoutWhereClause() throws SQLException
	{
		thrown.expect(SQLException.class);
		thrown.expectMessage("The UPDATE statement does not contain a valid WHERE clause.");
		getMutation("UPDATE FOO SET COL1=1, COL2=2");
	}

	@Test
	public void testUpdateStatementWithWhereClause() throws SQLException
	{
		Mutation updateMutation = getMutation("UPDATE FOO SET COL1=1, COL2=2 WHERE ID=1");
		Assert.assertNotNull(updateMutation);
		Assert.assertEquals(Op.UPDATE, updateMutation.getOperation());
		Assert.assertEquals("FOO", updateMutation.getTable());
		List<String> columns = Lists.newArrayList(updateMutation.getColumns());
		Assert.assertArrayEquals(new String[] { "COL1", "COL2", "ID" }, columns.toArray());
		Assert.assertArrayEquals(new String[] { "1", "2", "1" }, getValues(updateMutation.getValues()));
	}

	@Test
	public void testUpdateStatementWithMultipleWhereClauses() throws SQLException
	{
		Mutation updateMutation = getMutation("UPDATE FOO SET COL1=1, COL2=2 WHERE ID1=1 AND ID2=1");
		Assert.assertNotNull(updateMutation);
		Assert.assertEquals(Op.UPDATE, updateMutation.getOperation());
		Assert.assertEquals("FOO", updateMutation.getTable());
		List<String> columns = Lists.newArrayList(updateMutation.getColumns());
		Assert.assertArrayEquals(new String[] { "COL1", "COL2", "ID1", "ID2" }, columns.toArray());
		Assert.assertArrayEquals(new String[] { "1", "2", "1", "1" }, getValues(updateMutation.getValues()));
	}

	private String[] getValues(Iterable<Value> values)
	{
		List<Value> valueList = Lists.newArrayList(values);
		String[] res = new String[valueList.size()];
		int index = 0;
		for (Value value : valueList)
		{
			res[index] = value.toString();
			index++;
		}
		return res;
	}

	private Mutation getMutation(String sql) throws SQLException
	{
		Mutation mutation = null;
		CloudSpannerPreparedStatement ps = new CloudSpannerPreparedStatement(sql, null, null);
		try
		{
			Method createMutation = ps.getClass().getDeclaredMethod("createMutation");
			createMutation.setAccessible(true);
			mutation = (Mutation) createMutation.invoke(ps);
		}
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e)
		{
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e)
		{
			if (e.getTargetException() instanceof SQLException)
			{
				throw (SQLException) e.getTargetException();
			}
			throw new RuntimeException(e);
		}
		return mutation;
	}

}