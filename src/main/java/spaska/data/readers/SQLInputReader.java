package spaska.data.readers;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import spaska.data.Attribute;
import spaska.data.Attribute.ValueType;
import spaska.data.Dataset;
import spaska.data.Factory;
import spaska.data.Instance;
import spaska.data.Pair;
import spaska.data.Value;
import spaska.db.sql.SQLRoutines;

/**
 * @author psstoev
 * 
 */
public class SQLInputReader extends AbstractInputReader {

	private String tableName = null;
	private SQLRoutines sqlroutines = null;

	/**
	 * 
	 * @param tableName
	 * @param jdbcConnString
	 */
	public SQLInputReader(String tableName, String jdbcConnString) {
		this.tableName = tableName;
		this.sqlroutines = new SQLRoutines(jdbcConnString);
	}

	private ValueType getValueType(int valueCode) {
		switch (valueCode) {
		case Types.DOUBLE:
			return ValueType.Numeric;
		case Types.VARCHAR:
			return ValueType.Nominal;
		default:
			break;
		}
		return ValueType.Unknown;
	}

	private Set<Value> getDomain(String columnName) {
		Set<Value> domain = new HashSet<Value>();

		for (Value value : this.sqlroutines.getDomain(this.tableName,
				columnName)) {
			domain.add(value);
		}
		return domain;
	}

	private void handleAttributes() {
		ArrayList<Pair<String, Integer>> attributes = this.sqlroutines
				.getAttributes(this.tableName);

		for (Pair<String, Integer> attribute : attributes) {
			String name = attribute.getFirst();
			ValueType type = this.getValueType(attribute.getSecond());
			Attribute attr = new Attribute(name, type);

			this.getDataset().addAttribute(attr);
			if (type.equals(ValueType.Nominal)) {
				this.getDataset()
						.addAttributeDomain(attr, this.getDomain(name));
			}
		}
	}

	private void handleData() {
		ArrayList<String[]> instanceData = this.sqlroutines
				.getData(this.tableName);

		for (String[] data : instanceData) {
			List<Value> element = Factory.createElementData(data,
					this.getDataset());
			this.getDataset().addElement(new Instance(element));
		}

		for (Validator v : getValidators()) {
			v.validate();
		}
	}

	@Override
	public Dataset buildDataset() {
		setDataset(new Dataset());
		for (Validator v : getValidators()) {
			v.setDataset(getDataset());
		}

		getDataset().setName(this.tableName);
		this.handleAttributes();
		this.handleData();
		getDataset().setClassIndex(getDataset().getAttributesCount() - 1);
		return getDataset();
	}

	public static void main(String[] args) {
		System.out.println(new SQLInputReader("iris",
				"jdbc:mysql://localhost/spaska?user=spaska&password=spaska")
				.buildDataset().toString());
	}
}