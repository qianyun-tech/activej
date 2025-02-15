package io.activej.dataflow.calcite;

import io.activej.common.collection.CollectionUtils;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.activej.common.collection.CollectionUtils.concat;
import static io.activej.dataflow.calcite.utils.Utils.DATE_TIME_FORMATTER;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public abstract class AbstractCalciteTest extends CalciteTestBase {

	@Test
	public void testSelectAllStudents() {
		QueryResult result = query("""
			SELECT *
			FROM student
			""");

		QueryResult expected = studentsToQueryResult(concat(STUDENT_LIST_1, STUDENT_LIST_2));

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAllStudentsWithDuplicates() {
		QueryResult result = query("""
			SELECT *
			FROM student_duplicates
			""");

		QueryResult expected = studentsToQueryResult(new ArrayList<>(
			Stream.concat(
					STUDENT_DUPLICATES_LIST_1.stream(),
					STUDENT_DUPLICATES_LIST_2.stream()
				)
				.collect(Collectors.toMap(
					Student::id,
					Function.identity(),
					(student1, student2) -> student1.dept() > student2.dept() ? student1 : student2))
				.values()));

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAllStudentsWithDuplicatesNoPrimary() {
		QueryResult result = query("""
			SELECT *
			FROM student_duplicates_no_primary
			""");

		QueryResult expected = studentsToQueryResult(CollectionUtils.concat(
			STUDENT_DUPLICATES_NO_PRIMARY_LIST_1,
			STUDENT_DUPLICATES_NO_PRIMARY_LIST_2.subList(1, 2)
		));

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAllDepartments() {
		QueryResult result = query("""
			SELECT *
			FROM department
			""");

		QueryResult expected = departmentsToQueryResult(concat(DEPARTMENT_LIST_1, DEPARTMENT_LIST_2));

		assertEquals(expected, result);
	}

	@Test
	public void testSelectDepartmentsFields() {
		QueryResult result = query("""
			SELECT departmentName
			FROM department
			""");

		List<Object[]> columnValues = new ArrayList<>();
		for (Department department : concat(DEPARTMENT_LIST_1, DEPARTMENT_LIST_2)) {
			columnValues.add(new Object[]{department.departmentName()});
		}

		QueryResult expected = new QueryResult(List.of("departmentName"), columnValues);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectStudentsFields() {
		QueryResult result = query("""
			SELECT dept, firstName
			FROM student
			""");

		List<Object[]> columnValues = new ArrayList<>();
		for (Student student : concat(STUDENT_LIST_1, STUDENT_LIST_2)) {
			columnValues.add(new Object[]{student.dept(), student.firstName()});
		}

		QueryResult expected = new QueryResult(List.of("dept", "firstName"), columnValues);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectStudentFieldsRenamed() {
		QueryResult result = query("""
			SELECT dept as department, firstName as f_name, id as stud_id
			FROM student
			""");

		List<Object[]> columnValues = new ArrayList<>();
		for (Student student : concat(STUDENT_LIST_1, STUDENT_LIST_2)) {
			columnValues.add(new Object[]{student.dept(), student.firstName(), student.id()});
		}

		QueryResult expected = new QueryResult(List.of("department", "f_name", "stud_id"), columnValues);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectDepartmentFieldsAllRenamed() {
		QueryResult result = query("""
			SELECT id as dep_id, departmentName as dep_name
			FROM department
			""");

		List<Object[]> columnValues = new ArrayList<>();
		for (Department department : concat(DEPARTMENT_LIST_1, DEPARTMENT_LIST_2)) {
			columnValues.add(new Object[]{department.id(), department.departmentName()});
		}

		QueryResult expected = new QueryResult(List.of("dep_id", "dep_name"), columnValues);

		assertEquals(expected, result);
	}

	@Test
	public void testSubSelect() {
		QueryResult result = query("""
			SELECT firstName, id
			FROM (SELECT id, firstName, dept from student)
			""");

		List<Object[]> columnValues = new ArrayList<>();
		for (Student student : concat(STUDENT_LIST_1, STUDENT_LIST_2)) {
			columnValues.add(new Object[]{student.firstName(), student.id()});
		}

		QueryResult expected = new QueryResult(List.of("firstName", "id"), columnValues);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAdditionalColumns() {
		QueryResult result = query("""
			SELECT 123, 'test', id
			FROM department
			""");

		QueryResult expected = new QueryResult(
			List.of("123", "'test'", "id"),
			concat(DEPARTMENT_LIST_1, DEPARTMENT_LIST_2).stream()
				.map(department -> new Object[]{new BigDecimal(123), "test", department.id()})
				.toList()
		);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAdditionalColumnsNoTable() {
		QueryResult result = query("""
			SELECT 123, 'test1'
			UNION
			SELECT 321, 'test2'
			""");

		QueryResult expected = new QueryResult(
			List.of("123", "'test1'"),
			List.of(
				new Object[]{new BigDecimal(123), "test1"},
				new Object[]{new BigDecimal(321), "test2"}
			)
		);

		assertEquals(expected, result);
	}

	// region WhereEqualTrue
	@Test
	public void testWhereEqualTrue() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE 1 = 1
			""");

		assertWhereEqualTrue(result);
	}

	@Test
	public void testWhereEqualTruePrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE 1 = ?
				""",
			stmt -> stmt.setInt(1, 1));

		assertWhereEqualTrue(result);
	}

	private static void assertWhereEqualTrue(QueryResult result) {
		QueryResult expected = studentsToQueryResult(concat(STUDENT_LIST_1, STUDENT_LIST_2));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereEqualFalse
	@Test
	public void testWhereEqualFalse() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE 1 = 2
			""");

		assertWhereEqualFalse(result);
	}

	@Test
	public void testWhereEqualFalsePrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE 1 = ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertWhereEqualFalse(result);
	}

	private static void assertWhereEqualFalse(QueryResult result) {
		assertTrue(result.isEmpty());
	}
	// endregion

	// region WhereEqualScalar
	@Test
	public void testWhereEqualScalar() {
		QueryResult result = query("""
			SELECT firstName, lastName FROM student
			WHERE id = 2
			""");

		assertWhereEqualScalar(result);
	}

	@Test
	public void testWhereEqualScalarPrepared() {
		QueryResult result = queryPrepared("""
				SELECT firstName, lastName FROM student
				WHERE id = ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertWhereEqualScalar(result);
	}

	private static void assertWhereEqualScalar(QueryResult result) {
		Student student = STUDENT_LIST_2.get(1);
		QueryResult expected = new QueryResult(List.of("firstName", "lastName"),
			List.<Object[]>of(new Object[]{student.firstName(), student.lastName()}));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testWhereEqualField() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id = dept
			""");

		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(1), STUDENT_LIST_2.get(1)));

		assertEquals(expected, result);
	}

	// region WhereNotEqualTrue
	@Test
	public void testWhereNotEqualTrue() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE 1 <> 1
			""");

		assertWhereNotEqualTrue(result);
	}

	@Test
	public void testWhereNotEqualTruePrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE 1 <> ?
				""",
			stmt -> stmt.setInt(1, 1));

		assertWhereNotEqualTrue(result);
	}

	private static void assertWhereNotEqualTrue(QueryResult result) {
		assertTrue(result.isEmpty());
	}
	// endregion

	// region WhereNotEqualFalse
	@Test
	public void testWhereNotEqualFalse() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE 1 <> 2
			""");

		assertWhereNotEqualFalse(result);
	}

	@Test
	public void testWhereNotEqualFalsePrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE 1 <> ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertWhereNotEqualFalse(result);
	}

	private static void assertWhereNotEqualFalse(QueryResult result) {
		QueryResult expected = studentsToQueryResult(concat(STUDENT_LIST_1, STUDENT_LIST_2));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereNotEqualScalar
	@Test
	public void testWhereNotEqualScalar() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id <> 2
			""");

		assertWhereNotEqualScalar(result);
	}

	@Test
	public void testWhereNotEqualScalarPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id <> ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertWhereNotEqualScalar(result);
	}

	private static void assertWhereNotEqualScalar(QueryResult result) {
		QueryResult expected = studentsToQueryResult(concat(STUDENT_LIST_1, STUDENT_LIST_2.subList(0, 1)));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testWhereNotEqualField() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id <> dept
			""");

		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(0), STUDENT_LIST_1.get(2), STUDENT_LIST_2.get(0)));

		assertEquals(expected, result);
	}

	// region WhereGreaterThanScalar
	@Test
	public void testWhereGreaterThanScalar() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id > 1
			""");

		assertWhereGreaterThanScalar(result);
	}

	@Test
	public void testWhereGreaterThanScalarPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id > ?
				""",
			stmt -> stmt.setInt(1, 1));

		assertWhereGreaterThanScalar(result);
	}

	private static void assertWhereGreaterThanScalar(QueryResult result) {
		QueryResult expected = studentsToQueryResult(concat(concat(STUDENT_LIST_1.subList(0, 1), STUDENT_LIST_2), STUDENT_LIST_1.subList(2, 3)));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testWhereGreaterThanField() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id > dept
			""");

		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(0), STUDENT_LIST_2.get(0)));

		assertEquals(expected, result);
	}

	// region WhereGreaterThanOrEqualScalar
	@Test
	public void testWhereGreaterThanOrEqualScalar() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id >= 2
			""");

		assertWhereGreaterThanOrEqualScalar(result);
	}

	@Test
	public void testWhereGreaterThanOrEqualScalarPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id >= ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertWhereGreaterThanOrEqualScalar(result);
	}

	private static void assertWhereGreaterThanOrEqualScalar(QueryResult result) {
		QueryResult expected = studentsToQueryResult(concat(concat(STUDENT_LIST_1.subList(0, 1), STUDENT_LIST_1.subList(2, 3)), STUDENT_LIST_2));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testWhereGreaterThanOrEqualField() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE dept >= id
			""");

		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(1), STUDENT_LIST_1.get(2), STUDENT_LIST_2.get(1)));

		assertEquals(expected, result);
	}

	// region WhereLessThanScalar
	@Test
	public void testWhereLessThanScalar() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id < 2
			""");

		assertWhereLessThanScalar(result);
	}

	@Test
	public void testWhereLessThanScalarPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id < ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertWhereLessThanScalar(result);
	}

	private static void assertWhereLessThanScalar(QueryResult result) {
		QueryResult expected = studentsToQueryResult(STUDENT_LIST_1.subList(1, 2));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereLessThanOrEqualScalar
	@Test
	public void testWhereLessThanOrEqualScalar() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id <= 2
			""");

		assertWhereLessThanOrEqualScalar(result);
	}

	@Test
	public void testWhereLessThanOrEqualScalarPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id <= ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertWhereLessThanOrEqualScalar(result);
	}

	private static void assertWhereLessThanOrEqualScalar(QueryResult result) {
		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(1), STUDENT_LIST_2.get(1)));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereAndEqual
	@Test
	public void testWhereAndEqual() {
		QueryResult result = query("""
			SELECT id FROM student
			WHERE firstName = 'John' AND dept = 1
			""");

		assertWhereAndEqual(result);
	}

	@Test
	public void testWhereAndEqualPrepared() {
		QueryResult result = queryPrepared("""
				SELECT id FROM student
				WHERE firstName = ? AND dept = ?
				""",
			stmt -> {
				stmt.setString(1, "John");
				stmt.setInt(2, 1);
			});

		assertWhereAndEqual(result);
	}

	private static void assertWhereAndEqual(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"),
			List.<Object[]>of(new Object[]{STUDENT_LIST_1.get(1).id()}));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereOrEqual
	@Test
	public void testWhereOrEqual() {
		QueryResult result = query("""
			SELECT id FROM student
			WHERE firstName = 'Bob' OR dept = 2
			""");

		assertWhereOrEqual(result);
	}

	@Test
	public void testWhereOrEqualPrepared() {
		QueryResult result = queryPrepared("""
				SELECT id FROM student
				WHERE firstName = ? OR dept = ?
				""",
			stmt -> {
				stmt.setString(1, "Bob");
				stmt.setInt(2, 2);
			});

		assertWhereOrEqual(result);
	}

	private static void assertWhereOrEqual(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"),
			List.of(
				new Object[]{STUDENT_LIST_2.get(0).id()},
				new Object[]{STUDENT_LIST_2.get(1).id()}
			));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereBetween
	@Test
	public void testWhereBetween() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id BETWEEN 1 AND 2
			""");

		assertWhereBetween(result);
	}

	@Test
	public void testWhereBetweenPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id BETWEEN ? AND ?
				""",
			stmt -> {
				stmt.setInt(1, 1);
				stmt.setInt(2, 2);
			});

		assertWhereBetween(result);
	}

	private static void assertWhereBetween(QueryResult result) {
		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(1), STUDENT_LIST_2.get(1)));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereIn
	@Test
	public void testWhereIn() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id IN (1, 3, 6)
			""");

		assertWhereIn(result);
	}

	@Test
	public void testWhereInPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id IN (?, ?, ?)
				""",
			stmt -> {
				stmt.setInt(1, 1);
				stmt.setInt(2, 3);
				stmt.setInt(3, 6);
			});

		assertWhereIn(result);
	}

	private static void assertWhereIn(QueryResult result) {
		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(1), STUDENT_LIST_2.get(0)));

		assertEquals(expected, result);
	}
	// endregion

	// region WhereNotIn
	@Test
	public void testWhereNotIn() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE id NOT IN (1, 3, 6)
			""");

		assertWhereNotIn(result);
	}

	@Test
	public void testWhereNotInPrepared() {
		QueryResult result = queryPrepared("""
				SELECT * FROM student
				WHERE id NOT IN (?, ?, ?)
				""",
			stmt -> {
				stmt.setInt(1, 1);
				stmt.setInt(2, 3);
				stmt.setInt(3, 6);
			});

		assertWhereNotIn(result);
	}

	private static void assertWhereNotIn(QueryResult result) {
		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(0), STUDENT_LIST_1.get(2), STUDENT_LIST_2.get(1)));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testWhereLikeStartsWithJ() {
		doTestWhereLike("J%", STUDENT_LIST_1.get(1), STUDENT_LIST_2.get(0));
	}

	@Test
	public void testWhereNotLikeStartsWithJ() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE firstName NOT LIKE 'J%'
			""");

		QueryResult expected = studentsToQueryResult(Arrays.asList(STUDENT_LIST_1.get(0), STUDENT_LIST_1.get(2), STUDENT_LIST_2.get(1)));

		assertEquals(expected, result);
	}

	@Test
	public void testWhereLikeEndsWithHn() {
		doTestWhereLike("%hn", STUDENT_LIST_1.get(1), STUDENT_LIST_2.get(0));
	}

	@Test
	public void testWhereStartsWithB() {
		doTestWhereLike("B%", STUDENT_LIST_2.get(1));
	}

	@Test
	public void testWhereSecondLetterIsO() {
		doTestWhereLike("_o%", STUDENT_LIST_1.get(1), STUDENT_LIST_2.get(0), STUDENT_LIST_2.get(1));
	}

	@Test
	public void testWhereMatchAll() {
		doTestWhereLike("%", STUDENT_LIST_1.get(0), STUDENT_LIST_1.get(1), STUDENT_LIST_1.get(2), STUDENT_LIST_2.get(0), STUDENT_LIST_2.get(1));
	}

	private void doTestWhereLike(String firstNamePattern, Student... expectedStudents) {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE firstName LIKE '$pattern'
			"""
			.replace("$pattern", firstNamePattern));

		QueryResult expected = studentsToQueryResult(Arrays.asList(expectedStudents));

		assertEquals(expected, result);
	}

	@Test
	public void testWhereNoMatch() {
		QueryResult result = query("""
			SELECT * FROM student
			WHERE firstName LIKE 'A'
			""");

		assertTrue(result.isEmpty());
	}

	@Test
	public void testMapGetQuery() {
		QueryResult result = query("""
			SELECT id, MAP_GET(counters, 'John') FROM registry
			""");

		List<Object[]> columnValues = new ArrayList<>(result.columnValues.size());
		for (Registry registry : concat(REGISTRY_LIST_1, REGISTRY_LIST_2)) {
			Map<String, Integer> counters = registry.counters();
			columnValues.add(new Object[]{registry.id(), counters == null ? null : counters.get("John")});
		}

		QueryResult expected = new QueryResult(List.of("id", "counters.get('John')"), columnValues);

		assertEquals(expected, result);
	}

	// region MapGetInWhereClause
	@Test
	public void testMapGetInWhereClause() {
		QueryResult result = query("""
			SELECT id FROM registry
			WHERE MAP_GET(counters, 'Kevin') = 7
			""");

		assertMapGetInWhereClause(result);
	}

	@Test
	public void testMapGetInWhereClausePrepared() {
		QueryResult result = queryPrepared("""
				SELECT id FROM registry
				WHERE MAP_GET(counters, ?) = ?
				""",
			stmt -> {
				stmt.setString(1, "Kevin");
				stmt.setInt(2, 7);
			});

		assertMapGetInWhereClause(result);
	}

	private static void assertMapGetInWhereClause(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"),
			List.of(
				new Object[]{REGISTRY_LIST_1.get(1).id()},
				new Object[]{REGISTRY_LIST_2.get(0).id()}
			));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testMapIsNotNull() {
		QueryResult result = query("""
			SELECT id FROM registry
			WHERE counters IS NOT NULL
			""");

		QueryResult expected = new QueryResult(List.of("id"),
			List.of(
				new Object[]{REGISTRY_LIST_1.get(0).id()},
				new Object[]{REGISTRY_LIST_1.get(1).id()},
				new Object[]{REGISTRY_LIST_2.get(0).id()},
				new Object[]{REGISTRY_LIST_2.get(1).id()}
			));

		assertEquals(expected, result);
	}

	// region ListGetQuery
	@Test
	public void testListGetQuery() {
		QueryResult result = query("""
			SELECT id, LIST_GET(domains, 1) FROM registry
			""");

		assertListGetQuery(result, "1");
	}

	@Test
	public void testListGetQueryPrepared() {
		QueryResult result = queryPrepared("""
				SELECT id, LIST_GET(domains, ?) FROM registry
				""",
			stmt -> stmt.setInt(1, 1));

		assertListGetQuery(result, "?");
	}

	private static void assertListGetQuery(QueryResult result, String index) {
		List<Object[]> columnValues = new ArrayList<>(result.columnValues.size());
		for (Registry registry : concat(REGISTRY_LIST_1, REGISTRY_LIST_2)) {
			String domain = registry.domains().size() > 1 ? registry.domains().get(1) : null;
			columnValues.add(new Object[]{registry.id(), domain});
		}

		QueryResult expected = new QueryResult(List.of("id", "domains.get(" + index + ')'), columnValues);

		assertEquals(expected, result);
	}
	// endregion

	// region ListGetInWhereClause
	@Test
	public void testListGetInWhereClause() {
		QueryResult result = query("""
			SELECT id FROM registry
			WHERE LIST_GET(domains, 0) = 'google.com'
			""");

		assertListGetInWhereClause(result);
	}

	@Test
	public void testListGetInWhereClausePrepared() {
		QueryResult result = queryPrepared("""
				SELECT id FROM registry
				WHERE LIST_GET(domains, ?) = ?
				""",
			stmt -> {
				stmt.setInt(1, 0);
				stmt.setString(2, "google.com");
			});

		assertListGetInWhereClause(result);
	}

	private static void assertListGetInWhereClause(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"),
			List.of(
				new Object[]{REGISTRY_LIST_1.get(0).id()},
				new Object[]{REGISTRY_LIST_1.get(1).id()}
			));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testJoin() {
		QueryResult result = query("""
			SELECT *
			FROM student
			JOIN department
			ON student.dept = department.id
			""");

		assertFullStudentDepartmentJoin(result);
	}

	@Test
	public void testJoinNamedTables() {
		QueryResult result = query("""
			SELECT *
			FROM student s
			JOIN department d
			ON s.dept = d.id
			""");

		assertFullStudentDepartmentJoin(result);
	}

	@Test
	public void testJoinUsingWhere() {
		QueryResult result = query("""
			SELECT *
			FROM student, department
			WHERE student.dept = department.id
			""");

		assertFullStudentDepartmentJoin(result);
	}

	@Test
	public void testExplicitInnerJoin() {
		QueryResult result = query("""
			SELECT *
			FROM student
			INNER JOIN department
			ON student.dept = department.id
			""");

		assertFullStudentDepartmentJoin(result);
	}

	private static void assertFullStudentDepartmentJoin(QueryResult result) {
		List<Object[]> expectedColumnValues = new ArrayList<>(4);

		Student firstStudent = STUDENT_LIST_1.get(0);
		Student secondStudent = STUDENT_LIST_1.get(1);
		Student thirdStudent = STUDENT_LIST_2.get(0);
		Student fourthStudent = STUDENT_LIST_2.get(1);
		Department firstDepartment = DEPARTMENT_LIST_1.get(0);
		Department secondDepartment = DEPARTMENT_LIST_2.get(0);
		Department thirdDepartment = DEPARTMENT_LIST_2.get(1);

		expectedColumnValues.add(new Object[]{
			firstStudent.id(), firstStudent.firstName(), firstStudent.lastName(), firstStudent.dept(),
			secondDepartment.id(), secondDepartment.departmentName(), secondDepartment.aliases()
		});
		expectedColumnValues.add(new Object[]{
			secondStudent.id(), secondStudent.firstName(), secondStudent.lastName(), secondStudent.dept(),
			thirdDepartment.id(), thirdDepartment.departmentName(), thirdDepartment.aliases()
		});
		expectedColumnValues.add(new Object[]{
			thirdStudent.id(), thirdStudent.firstName(), thirdStudent.lastName(), thirdStudent.dept(),
			firstDepartment.id(), firstDepartment.departmentName(), firstDepartment.aliases()
		});
		expectedColumnValues.add(new Object[]{
			fourthStudent.id(), fourthStudent.firstName(), fourthStudent.lastName(), fourthStudent.dept(),
			firstDepartment.id(), firstDepartment.departmentName(), firstDepartment.aliases()
		});

		QueryResult expected = new QueryResult(List.of("id", "firstName", "lastName", "dept", "id0", "departmentName", "aliases"), expectedColumnValues);

		assertEquals(expected, result);
	}

	@Test
	public void testJoinByFields() {
		QueryResult result = query("""
			SELECT student.id, department.departmentName FROM student
			JOIN department
			ON student.dept = department.id
			""");

		Student firstStudent = STUDENT_LIST_1.get(0);
		Student secondStudent = STUDENT_LIST_1.get(1);
		Student thirdStudent = STUDENT_LIST_2.get(0);
		Student fourthStudent = STUDENT_LIST_2.get(1);
		Department firstDepartment = DEPARTMENT_LIST_1.get(0);
		Department secondDepartment = DEPARTMENT_LIST_2.get(0);
		Department thirdDepartment = DEPARTMENT_LIST_2.get(1);

		QueryResult expected = new QueryResult(List.of("id", "departmentName"),
			List.of(
				new Object[]{firstStudent.id(), secondDepartment.departmentName()},
				new Object[]{secondStudent.id(), thirdDepartment.departmentName()},
				new Object[]{thirdStudent.id(), firstDepartment.departmentName()},
				new Object[]{fourthStudent.id(), firstDepartment.departmentName()}
			));

		assertEquals(expected, result);
	}

	@Test
	public void testSelfJoin() {
		QueryResult result = query("""
			SELECT *
			FROM student s1
			JOIN student s2
			ON s1.id = s2.id
			JOIN student s3
			ON s3.id = s1.id
			""");

		assertSelfJoinStudent(result);
	}

	@Test
	public void testSelfJoinDifferentColumns() {
		QueryResult result = query("""
			SELECT *
			FROM student s1
			JOIN student s2
			ON s1.id = s2.id
			JOIN student s3
			ON s3.lastName = s1.lastName
			""");

		assertSelfJoinStudent(result);
	}

	private static void assertSelfJoinStudent(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id", "firstName", "lastName", "dept", "id0", "firstName0", "lastName0", "dept0", "id1", "firstName1", "lastName1", "dept1"),
			concat(STUDENT_LIST_1, STUDENT_LIST_2).stream()
				.map(student -> {
					Object[] row = new Object[12];
					for (int i = 0; i < 3; i++) {
						int offset = i * 4;
						row[offset] = student.id();
						row[offset + 1] = student.firstName();
						row[offset + 2] = student.lastName();
						row[offset + 3] = student.dept();
					}
					return row;
				})
				.toList());

		assertEquals(expected, result);
	}

	@Test
	public void testJoinByMultipleColumns() {
		QueryResult result = query("""
			SELECT s.lastName, s.firstName, p.courseCode, p.status, p.amount
			FROM enrollment e
			JOIN student s
			ON s.id = e.studentId
			JOIN payment p
			ON p.courseCode = e.courseCode
			AND p.studentId = e.studentId
			""");

		Student firstStudent = STUDENT_LIST_1.get(1);
		Student secondStudent = STUDENT_LIST_2.get(1);
		Student thirdStudent = STUDENT_LIST_2.get(0);

		Payment firstPayment = PAYMENT_LIST_1.get(0);
		Payment secondPayment = PAYMENT_LIST_2.get(0);
		Payment thirdPayment = PAYMENT_LIST_1.get(1);
		Payment fourthPayment = PAYMENT_LIST_2.get(1);

		QueryResult expected = new QueryResult(List.of("lastName", "firstName", "courseCode", "status", "amount"),
			List.of(
				new Object[]{firstStudent.lastName(), firstStudent.firstName(), firstPayment.courseCode(), firstPayment.status(), firstPayment.amount()},
				new Object[]{firstStudent.lastName(), firstStudent.firstName(), secondPayment.courseCode(), secondPayment.status(), secondPayment.amount()},
				new Object[]{secondStudent.lastName(), secondStudent.firstName(), thirdPayment.courseCode(), thirdPayment.status(), thirdPayment.amount()},
				new Object[]{thirdStudent.lastName(), thirdStudent.firstName(), fourthPayment.courseCode(), fourthPayment.status(), fourthPayment.amount()}
			));

		assertEquals(expected, result);
	}

	@Test
	public void testLeftJoin() {
		QueryResult result = query("""
			SELECT *
			FROM student
			LEFT JOIN department
			ON student.dept = department.id
			""");

		List<Object[]> expectedColumnValues = new ArrayList<>(4);

		Student firstStudent = STUDENT_LIST_1.get(0);
		Student secondStudent = STUDENT_LIST_1.get(1);
		Student thirdStudent = STUDENT_LIST_1.get(2);
		Student fourthStudent = STUDENT_LIST_2.get(0);
		Student fifthStudent = STUDENT_LIST_2.get(1);
		Department firstDepartment = DEPARTMENT_LIST_1.get(0);
		Department thirdDepartment = DEPARTMENT_LIST_2.get(0);
		Department fourthDepartment = DEPARTMENT_LIST_2.get(1);

		expectedColumnValues.add(new Object[]{
			firstStudent.id(), firstStudent.firstName(), firstStudent.lastName(), firstStudent.dept(),
			thirdDepartment.id(), thirdDepartment.departmentName(), thirdDepartment.aliases()
		});
		expectedColumnValues.add(new Object[]{
			secondStudent.id(), secondStudent.firstName(), secondStudent.lastName(), secondStudent.dept(),
			fourthDepartment.id(), fourthDepartment.departmentName(), fourthDepartment.aliases()
		});
		expectedColumnValues.add(new Object[]{
			fourthStudent.id(), fourthStudent.firstName(), fourthStudent.lastName(), fourthStudent.dept(),
			firstDepartment.id(), firstDepartment.departmentName(), firstDepartment.aliases()
		});
		expectedColumnValues.add(new Object[]{
			fifthStudent.id(), fifthStudent.firstName(), fifthStudent.lastName(), fifthStudent.dept(),
			firstDepartment.id(), firstDepartment.departmentName(), firstDepartment.aliases()
		});
		expectedColumnValues.add(new Object[]{
			thirdStudent.id(), thirdStudent.firstName(), thirdStudent.lastName(), thirdStudent.dept(),
			null, null, null
		});

		QueryResult expected = new QueryResult(List.of("id", "firstName", "lastName", "dept", "id0", "departmentName", "aliases"), expectedColumnValues);

		assertEquals(expected, result);
	}

	@Test
	public void testOrderBy() {
		QueryResult result = query("""
			SELECT * FROM student
			ORDER BY firstName ASC, id DESC
			""");

		QueryResult expected = studentsToQueryResult(List.of(
			STUDENT_LIST_2.get(1),
			STUDENT_LIST_2.get(0),
			STUDENT_LIST_1.get(1),
			STUDENT_LIST_1.get(0),
			STUDENT_LIST_1.get(2)));

		assertTrue(expected.equalsOrdered(result));
	}

	@Test
	public void testOrderByFunctionCall() {
		QueryResult result = query("""
			SELECT * FROM registry
			ORDER BY MAP_GET(counters, 'Kevin'), id
			""");

		QueryResult expected = registryToQueryResult(
			List.of(
				REGISTRY_LIST_1.get(1),
				REGISTRY_LIST_2.get(0),
				REGISTRY_LIST_1.get(0),
				REGISTRY_LIST_2.get(1),
				REGISTRY_LIST_1.get(2)
			)
		);

		assertTrue(expected.equalsOrdered(result));
	}

	@Test
	public void testOrderByFunctionCallNullsLast() {
		QueryResult result = query("""
			SELECT * FROM registry
			WHERE counters IS NOT NULL
			ORDER BY MAP_GET(counters, 'Kevin'), id
			NULLS LAST
			""");

		QueryResult expected = registryToQueryResult(
			List.of(
				REGISTRY_LIST_1.get(1),
				REGISTRY_LIST_2.get(0),
				REGISTRY_LIST_1.get(0),
				REGISTRY_LIST_2.get(1)
			)
		);

		assertTrue(expected.equalsOrdered(result));
	}

	@Test
	public void testOrderByFunctionCallNullsFirst() {
		QueryResult result = query("""
			SELECT * FROM registry
			WHERE counters IS NOT NULL
			ORDER BY MAP_GET(counters, 'John')
			NULLS FIRST
			""");

		QueryResult expected = registryToQueryResult(concat(REGISTRY_LIST_1.subList(0, 2), REGISTRY_LIST_2));

		assertTrue(expected.equalsOrdered(result));
	}

	@Test
	public void testOrderByNonReturnedField() {
		QueryResult result = query("""
			SELECT lastName FROM student
			ORDER BY firstName ASC, id DESC
			""");

		QueryResult expected = new QueryResult(
			List.of("lastName"),
			Stream.of(STUDENT_LIST_2.get(1), STUDENT_LIST_2.get(0), STUDENT_LIST_1.get(1), STUDENT_LIST_1.get(0), STUDENT_LIST_1.get(2))
				.map(student -> new Object[]{student.lastName()})
				.toList()
		);

		assertTrue(expected.equalsOrdered(result));
	}

	@Test
	public void testPojoFieldSelect() {
		QueryResult result = query("""
			SELECT id, profiles.pojo.value1, profiles.pojo.value2 FROM profiles
			""");

		List<Object[]> expectedColumnValues = new ArrayList<>();

		for (UserProfile userProfile : concat(USER_PROFILES_LIST_1, USER_PROFILES_LIST_2)) {
			expectedColumnValues.add(new Object[]{
				userProfile.id(),
				userProfile.pojo().value1(),
				userProfile.pojo().value2()
			});
		}

		QueryResult expected = new QueryResult(List.of("id", "pojo.value1", "pojo.value2"), expectedColumnValues);

		assertEquals(expected, result);
	}

	// region PojoFieldInWhereClause
	@Test
	public void testPojoFieldInWhereClause() {
		QueryResult result = query("""
			SELECT id
			FROM profiles
			WHERE profiles.pojo.value1 = 'test1'
			""");

		assertPojoFieldInWhereClause(result);
	}

	@Test
	public void testPojoFieldInWhereClausePrepared() {
		QueryResult result = queryPrepared("""
				SELECT id
				FROM profiles
				WHERE profiles.pojo.value1 = ?
				""",
			stmt -> stmt.setString(1, "test1"));

		assertPojoFieldInWhereClause(result);
	}

	private static void assertPojoFieldInWhereClause(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"), List.<Object[]>of(new Object[]{USER_PROFILES_LIST_2.get(0).id()}));

		assertEquals(expected, result);
	}
	// endregion

	// region UserProfilesSelect
	@Test
	public void testUserProfilesSelect() {
		QueryResult result = query("""
			SELECT id, MAP_GET(intents, 1).keyword, MAP_GET(intents, 1).matchType FROM profiles
			""");

		assertUserProfilesSelect(result, "1");
	}

	@Test
	public void testUserProfilesSelectPrepared() {
		QueryResult result = queryPrepared("""
				SELECT id, MAP_GET(intents, ?).keyword, MAP_GET(intents, ?).matchType FROM profiles
				""",
			stmt -> {
				stmt.setInt(1, 1);
				stmt.setInt(2, 1);
			});

		assertUserProfilesSelect(result, "?");
	}

	private static void assertUserProfilesSelect(QueryResult result, String key) {
		List<Object[]> expectedColumnValues = new ArrayList<>();

		for (UserProfile userProfile : concat(USER_PROFILES_LIST_1, USER_PROFILES_LIST_2)) {
			UserProfileIntent intent = userProfile.intents().get(1);
			expectedColumnValues.add(new Object[]{
				userProfile.id(),
				intent == null ? null : intent.keyword(),
				intent == null ? null : intent.matchType()
			});
		}

		QueryResult expected = new QueryResult(List.of("id", "intents.get(" + key + ").keyword", "intents.get(" + key + ").matchType"), expectedColumnValues);

		assertEquals(expected, result);
	}
	// endregion

	// region UserProfilesInWhereClause
	@Test
	public void testUserProfilesInWhereClause() {
		QueryResult result = query("""
			SELECT id
			FROM profiles
			WHERE MAP_GET(intents, 1).keyword = 'test1'
			""");

		assertUserProfilesInWhereClause(result);
	}

	@Test
	public void testUserProfilesInWhereClausePrepared() {
		QueryResult result = queryPrepared("""
				SELECT id
				FROM profiles
				WHERE MAP_GET(intents, ?).keyword = ?
				""",
			stmt -> {
				stmt.setInt(1, 1);
				stmt.setString(2, "test1");
			});

		assertUserProfilesInWhereClause(result);
	}

	private static void assertUserProfilesInWhereClause(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"), List.<Object[]>of(new Object[]{USER_PROFILES_LIST_2.get(0).id()}));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testCountAllStudents() {
		QueryResult result = query("""
			SELECT COUNT(*)
			FROM student
			""");

		QueryResult expected = new QueryResult(List.of("COUNT(*)"), List.<Object[]>of(new Object[]{5L}));

		assertEquals(expected, result);
	}

	@Test
	public void testCountAllStudentsRenamed() {
		QueryResult result = query("""
			SELECT COUNT(*) as student_count
			FROM student
			""");

		QueryResult expected = new QueryResult(List.of("student_count"), List.<Object[]>of(new Object[]{5L}));

		assertEquals(expected, result);
	}

	@Test
	public void testCountSumAllStudents() {
		QueryResult result = query("""
			SELECT COUNT(*), SUM(id)
			FROM student
			""");

		QueryResult expected = new QueryResult(List.of("COUNT(*)", "SUM(id)"), List.<Object[]>of(new Object[]{5L, 15L}));

		assertEquals(expected, result);
	}

	// region CountMapGet
	@Test
	public void testCountMapGet() {
		QueryResult result = query("""
			SELECT COUNT(MAP_GET(counters, 'John'))
			FROM registry
			""");

		assertCountMapGet(result, "'John'");
	}

	@Test
	public void testCountMapGetPrepared() {
		QueryResult result = queryPrepared("""
				SELECT COUNT(MAP_GET(counters, ?))
				FROM registry
				""",
			stmt -> stmt.setString(1, "John"));

		assertCountMapGet(result, "?");
	}

	private static void assertCountMapGet(QueryResult result, String key) {
		QueryResult expected = new QueryResult(List.of("COUNT(counters.get(" + key + "))"), List.<Object[]>of(new Object[]{3L}));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testSumStudentsId() {
		QueryResult result = query("""
			SELECT SUM(id)
			FROM student
			""");

		QueryResult expected = new QueryResult(List.of("SUM(id)"), List.<Object[]>of(new Object[]{15L}));

		assertEquals(expected, result);
	}

	// region SumPojoValues
	@Test
	public void testSumPojoValues() {
		QueryResult result = query("""
			SELECT SUM(MAP_GET(intents, 2).campaignId)
			FROM profiles
			""");

		assertSumPojoValues(result, "2");
	}

	@Test
	public void testSumPojoValuesPrepared() {
		QueryResult result = queryPrepared("""
				SELECT SUM(MAP_GET(intents, ?).campaignId)
				FROM profiles
				""",
			stmt -> stmt.setInt(1, 2));

		assertSumPojoValues(result, "?");
	}

	private static void assertSumPojoValues(QueryResult result, String key) {
		QueryResult expected = new QueryResult(List.of("SUM(intents.get(" + key + ").campaignId)"), List.<Object[]>of(new Object[]{4L}));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testAvgStudentsDepts() {
		QueryResult result = query("""
			SELECT AVG(dept)
			FROM student
			""");

		QueryResult expected = new QueryResult(List.of("AVG(dept)"), List.<Object[]>of(new Object[]{3.6d}));

		assertEquals(expected, result);
	}

	@Test
	public void testMinStudentsId() {
		QueryResult result = query("""
			SELECT MIN(id)
			FROM student
			""");

		QueryResult expected = new QueryResult(List.of("MIN(id)"), List.<Object[]>of(new Object[]{1}));

		assertEquals(expected, result);
	}

	@Test
	public void testMaxStudentsId() {
		QueryResult result = query("""
			SELECT MAX(id)
			FROM student
			""");

		QueryResult expected = new QueryResult(List.of("MAX(id)"), List.<Object[]>of(new Object[]{5}));

		assertEquals(expected, result);
	}

	// region SelectPojo
	@Test
	public void testSelectPojo() {
		QueryResult result = query("""
			SELECT pojo
			FROM profiles
			WHERE id = 'user1'
			""");

		assertSelectPojo(result);
	}

	@Test
	public void testSelectPojoPrepared() {
		QueryResult result = queryPrepared("""
				SELECT pojo
				FROM profiles
				WHERE id = ?
				""",
			stmt -> stmt.setString(1, "user1"));

		assertSelectPojo(result);
	}

	@Test
	public void testSelectPojoPreparedRepeated() {
		List<QueryResult> results = queryPreparedRepeated("""
				SELECT pojo
				FROM profiles
				WHERE id = ?
				""",
			stmt -> stmt.setString(1, "user1"),
			stmt -> stmt.setString(1, "user2"));

		List<QueryResult> expected = List.of(
			new QueryResult(List.of("pojo"), List.<Object[]>of(new Object[]{new UserProfilePojo("test1", 1)})),
			new QueryResult(List.of("pojo"), List.<Object[]>of(new Object[]{new UserProfilePojo("test2", 2)}))
		);

		assertEquals(expected, results);
	}

	private static void assertSelectPojo(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("pojo"), List.<Object[]>of(new Object[]{new UserProfilePojo("test1", 1)}));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testSelectAllLarge() {
		QueryResult result = query("""
			SELECT *
			FROM large_table
			""");

		QueryResult expected = largeToQueryResult(concat(LARGE_LIST_1, LARGE_LIST_2));

		assertEquals(expected, result);
	}

	@Test
	public void testSelectIsNull() {
		QueryResult result = query("""
			SELECT id
			FROM student
			WHERE lastName IS NULL
			""");

		QueryResult expected = new QueryResult(List.of("id"), List.<Object[]>of(new Object[]{4}));

		assertEquals(expected, result);
	}

	@Test
	public void testSelectIsNotNull() {
		QueryResult result = query("""
			SELECT *
			FROM student
			WHERE lastName IS NOT NULL
			""");

		QueryResult expected = studentsToQueryResult(concat(STUDENT_LIST_1.subList(1, 3), STUDENT_LIST_2));

		assertEquals(expected, result);
	}

	@Test
	public void testSelectIfNull() {
		QueryResult result = query("""
			SELECT id, IFNULL(lastName, '')
			FROM student
			""");

		QueryResult expected = new QueryResult(
			List.of("id", "IFNULL(lastName, '')"),
			List.of(
				new Object[]{4, ""},
				new Object[]{1, "Doe"},
				new Object[]{3, "Truman"},
				new Object[]{2, "Black"},
				new Object[]{5, "Moore"}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testUnion() {
		QueryResult result = query("""
			SELECT id, firstName
			FROM student
			WHERE id <= 2
			UNION
			SELECT id, departmentName
			FROM department
			UNION
			SELECT id, lastName
			FROM student
			WHERE id > 2
			""");

		QueryResult expected = new QueryResult(
			List.of("id", "firstName"),
			List.of(
				new Object[]{STUDENT_LIST_1.get(0).id(), STUDENT_LIST_1.get(0).lastName()},
				new Object[]{STUDENT_LIST_1.get(1).id(), STUDENT_LIST_1.get(1).firstName()},
				new Object[]{STUDENT_LIST_1.get(2).id(), STUDENT_LIST_1.get(2).lastName()},
				new Object[]{STUDENT_LIST_2.get(0).id(), STUDENT_LIST_2.get(0).lastName()},
				new Object[]{STUDENT_LIST_2.get(1).id(), STUDENT_LIST_2.get(1).firstName()},
				new Object[]{DEPARTMENT_LIST_1.get(0).id(), DEPARTMENT_LIST_1.get(0).departmentName()},
				new Object[]{DEPARTMENT_LIST_2.get(0).id(), DEPARTMENT_LIST_2.get(0).departmentName()},
				new Object[]{DEPARTMENT_LIST_2.get(1).id(), DEPARTMENT_LIST_2.get(1).departmentName()}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testUnionDeduplication() {
		QueryResult result = query("""
			SELECT id
			FROM student
			UNION
			SELECT id
			FROM department
			""");

		QueryResult expected = new QueryResult(
			List.of("id"),
			List.of(
				new Object[]{1},
				new Object[]{2},
				new Object[]{3},
				new Object[]{4},
				new Object[]{5}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testUnionAll() {
		QueryResult result = query("""
			SELECT id
			FROM student
			UNION ALL
			SELECT id
			FROM department
			""");

		QueryResult expected = new QueryResult(
			List.of("id"),
			List.of(
				new Object[]{1},
				new Object[]{2},
				new Object[]{3},
				new Object[]{4},
				new Object[]{1},
				new Object[]{2},
				new Object[]{3},
				new Object[]{5}
			)
		);

		assertEquals(expected, result);
	}

	// region Offset
	@Test
	public void testOffset() {
		QueryResult result = query("""
			SELECT *
			FROM student
			OFFSET 2
			""");

		assertOffset(result);
	}

	@Test
	public void testOffsetPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM student
				OFFSET ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertOffset(result);
	}

	private static void assertOffset(QueryResult result) {
		assertEquals(List.of("id", "firstName", "lastName", "dept"), result.columnNames);

		assertEquals(STUDENT_LIST_1.size() + STUDENT_LIST_2.size() - 2, result.columnValues.size());

		Set<Student> returned = new HashSet<>();
		for (Object[] columnValue : result.columnValues) {
			Student student = columnsToStudent(columnValue);

			assertTrue(STUDENT_LIST_1.contains(student) || STUDENT_LIST_2.contains(student));

			assertTrue(returned.add(student));
		}
	}
	// endregion

	// region Limit
	@Test
	public void testLimit() {
		QueryResult result = query("""
			SELECT *
			FROM student
			LIMIT 2
			""");

		assertLimit(result);
	}

	@Test
	public void testLimitPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM student
				LIMIT ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertLimit(result);
	}

	private static void assertLimit(QueryResult result) {
		assertEquals(List.of("id", "firstName", "lastName", "dept"), result.columnNames);

		assertEquals(2, result.columnValues.size());

		Set<Student> returned = new HashSet<>();
		for (Object[] columnValue : result.columnValues) {
			Student student = columnsToStudent(columnValue);

			assertTrue(STUDENT_LIST_1.contains(student) || STUDENT_LIST_2.contains(student));

			assertTrue(returned.add(student));
		}
	}
	// endregion

	// region OffsetLimit
	@Test
	public void testOffsetLimit() {
		QueryResult result = query("""
			SELECT *
			FROM student
			LIMIT 2
			OFFSET 1
			""");

		assertOffsetLimit(result);
	}

	@Test
	public void testOffsetLimitPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM student
				LIMIT ?
				OFFSET ?
				""",
			stmt -> {
				stmt.setInt(1, 2);
				stmt.setInt(2, 1);
			});

		assertOffsetLimit(result);
	}

	private static void assertOffsetLimit(QueryResult result) {
		assertEquals(List.of("id", "firstName", "lastName", "dept"), result.columnNames);

		assertEquals(Math.min(2, STUDENT_LIST_1.size() + STUDENT_LIST_2.size() - 1), result.columnValues.size());

		Set<Student> returned = new HashSet<>();
		for (Object[] columnValue : result.columnValues) {
			Student student = columnsToStudent(columnValue);

			assertTrue(STUDENT_LIST_1.contains(student) || STUDENT_LIST_2.contains(student));

			assertTrue(returned.add(student));
		}
	}
	// endregion

	// region SortedOffset
	@Test
	public void testSortedOffset() {
		QueryResult result = query("""
			SELECT *
			FROM student
			ORDER BY firstName ASC, id DESC
			OFFSET 2
			""");

		assertSortedOffset(result);
	}

	@Test
	public void testSortedOffsetPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM student
				ORDER BY firstName ASC, id DESC
				OFFSET ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertSortedOffset(result);
	}

	private static void assertSortedOffset(QueryResult result) {
		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_1.get(1), STUDENT_LIST_1.get(0), STUDENT_LIST_1.get(2)));

		assertTrue(expected.equalsOrdered(result));
	}
	// endregion

	// region SortedLimit
	@Test
	public void testSortedLimit() {
		QueryResult result = query("""
			SELECT *
			FROM student
			ORDER BY firstName ASC, id DESC
			LIMIT 2
			""");

		assertSortedLimit(result);
	}

	@Test
	public void testSortedLimitPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM student
				ORDER BY firstName ASC, id DESC
				LIMIT ?
				""",
			stmt -> stmt.setInt(1, 2));

		assertSortedLimit(result);
	}

	private static void assertSortedLimit(QueryResult result) {
		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_2.get(1), STUDENT_LIST_2.get(0)));

		assertTrue(expected.equalsOrdered(result));
	}
	// endregion

	// region SortedOffsetLimit
	@Test
	public void testSortedOffsetLimit() {
		QueryResult result = query("""
			SELECT *
			FROM student
			ORDER BY firstName ASC, id DESC
			LIMIT 2
			OFFSET 1
			""");

		assertSortedOffsetLimit(result);
	}

	@Test
	public void testSortedOffsetLimitPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM student
				ORDER BY firstName ASC, id DESC
				LIMIT ?
				OFFSET ?
				""",
			stmt -> {
				stmt.setInt(1, 2);
				stmt.setInt(2, 1);
			});

		assertSortedOffsetLimit(result);
	}

	private static void assertSortedOffsetLimit(QueryResult result) {
		QueryResult expected = studentsToQueryResult(List.of(STUDENT_LIST_2.get(0), STUDENT_LIST_1.get(1)));

		assertTrue(expected.equalsOrdered(result));
	}
	// endregion

	@Test
	public void testGroupBySingleColumn() {
		QueryResult result = query("""
			SELECT subject, COUNT(*)
			FROM subject_selection
			GROUP BY subject
			""");

		QueryResult expected = new QueryResult(
			List.of("subject", "COUNT(*)"),
			List.of(
				new Object[]{"ITB001", 5L},
				new Object[]{"MKB114", 4L}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testGroupBySingleColumnReverseOrder() {
		QueryResult result = query("""
			SELECT COUNT(*), subject
			FROM subject_selection
			GROUP BY subject
			""");

		QueryResult expected = new QueryResult(
			List.of("COUNT(*)", "subject"),
			List.of(
				new Object[]{5L, "ITB001"},
				new Object[]{4L, "MKB114"}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testGroupBySingleColumnWithHaving() {
		QueryResult result = query("""
			SELECT subject, COUNT(*)
			FROM subject_selection
			GROUP BY subject
			HAVING COUNT(*) > 4
			""");

		QueryResult expected = new QueryResult(
			List.of("subject", "COUNT(*)"),
			List.<Object[]>of(new Object[]{"ITB001", 5L})
		);

		assertEquals(expected, result);
	}

	@Test
	public void testGroupByMultipleColumns() {
		QueryResult result = query("""
			SELECT subject, semester, COUNT(*)
			FROM subject_selection
			GROUP BY subject, semester
			""");

		QueryResult expected = new QueryResult(
			List.of("subject", "semester", "COUNT(*)"),
			List.of(
				new Object[]{"ITB001", 1, 3L},
				new Object[]{"ITB001", 2, 2L},
				new Object[]{"MKB114", 1, 3L},
				new Object[]{"MKB114", 2, 1L}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testGroupByMultipleColumnsWithHaving() {
		QueryResult result = query("""
			SELECT subject, semester, COUNT(*)
			FROM subject_selection
			GROUP BY subject, semester
			HAVING COUNT(*) > 1
			""");

		QueryResult expected = new QueryResult(
			List.of("subject", "semester", "COUNT(*)"),
			List.of(
				new Object[]{"ITB001", 1, 3L},
				new Object[]{"ITB001", 2, 2L},
				new Object[]{"MKB114", 1, 3L}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testFilterable() {
		QueryResult result = query("""
			SELECT id
			FROM filterable
			WHERE created > 20 AND created < 60
			""");

		QueryResult expected = new QueryResult(
			List.of("id"),
			List.of(
				new Object[]{45},
				new Object[]{64}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testFailing() {
		AssertionError e = assertThrows(AssertionError.class, () -> query("""
			SELECT *
			FROM failing
			"""));

		Throwable cause = e.getCause();
		assertThat(cause.getMessage(), containsString(FAILING_TABLE_EXCEPTION.getMessage()));
	}

	@Test
	public void testFailingFiltered() {
		AssertionError e = assertThrows(AssertionError.class, () -> query("""
			SELECT *
			FROM failing_filtered
			"""));

		Throwable cause = e.getCause();
		assertThat(cause.getMessage(), containsString(FAILING_TABLE_EXCEPTION.getMessage()));
	}

	@Test
	public void testComplexQuery() {
		QueryResult result = query("""
			SELECT uname, SUM(nameCount) as totalNameCount
			FROM (
				SELECT 'John' as uname, MAP_GET(counters, 'John') as nameCount
				FROM registry
				UNION
				SELECT 'Kevin', MAP_GET(counters, 'Kevin')
				FROM registry
				)
			GROUP BY uname
			""");

		QueryResult expected = new QueryResult(
			List.of("uname", "totalNameCount"),
			List.of(
				new Object[]{"John", 1018L},
				new Object[]{"Kevin", 15L}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAllCustom() {
		QueryResult result = query("""
			SELECT *
			FROM custom
			""");

		QueryResult expected = new QueryResult(
			List.of("id", "price", "description"),
			List.of(
				new Object[]{1, 32.4d, "abc"},
				new Object[]{2, 43.53d, "ijk"},
				new Object[]{3, 9.4343d, "geh"},
				new Object[]{4, 102.42d, "def"}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAllCustomPartitioned() {
		QueryResult result = query("""
			SELECT *
			FROM custom_partitioned
			""");

		QueryResult expected = new QueryResult(
			List.of("id", "price"),
			List.of(
				new Object[]{1, 32.4d},
				new Object[]{2, 43.53d},
				new Object[]{3, 9.4343d},
				new Object[]{4, 102.42d},
				new Object[]{5, 231.424d}
			)
		);

		assertEquals(expected, result);
	}

	@Test
	public void testSelectAllTemporalValues() {
		QueryResult result = query("""
			SELECT *
			FROM temporal_values
			""");

		QueryResult expected = temporalValuesToQueryResult(concat(TEMPORAL_VALUES_LIST_1, TEMPORAL_VALUES_LIST_2));

		assertEquals(expected, result);
	}

	// region SelectTemporalValuesByTimestamp
	@Test
	public void testSelectTemporalValuesByTimestamp() {
		QueryResult result = query("""
			SELECT *
			FROM temporal_values
			WHERE registeredAt > '2022-06-01 12:34:23'
			""");

		assertSelectTemporalValuesByTimestamp(result);
	}

	@Test
	public void testSelectTemporalValuesByTimestampPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM temporal_values
				WHERE registeredAt > ?
				""",
			stmt -> stmt.setObject(1, LocalDateTime.parse("2022-06-01 12:34:23", DATE_TIME_FORMATTER)));

		assertSelectTemporalValuesByTimestamp(result);
	}

	private void assertSelectTemporalValuesByTimestamp(QueryResult result) {
		QueryResult expected = temporalValuesToQueryResult(List.of(TEMPORAL_VALUES_LIST_1.get(0), TEMPORAL_VALUES_LIST_2.get(1)));

		assertEquals(expected, result);
	}
	// endregion

	// region SelectTemporalValuesByTimestampEquals
	@Test
	public void testSelectTemporalValuesByTimestampEquals() {
		QueryResult result = query("""
			SELECT *
			FROM temporal_values
			WHERE registeredAt = '2022-06-15 12:00:00'
			""");

		assertSelectTemporalValuesByTimestampEquals(result);
	}

	@Test
	public void testSelectTemporalValuesByTimestampPreparedEquals() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM temporal_values
				WHERE registeredAt = ?
				""",
			stmt -> stmt.setObject(1, LocalDateTime.parse("2022-06-15 12:00:00", DATE_TIME_FORMATTER)));

		assertSelectTemporalValuesByTimestampEquals(result);
	}

	private void assertSelectTemporalValuesByTimestampEquals(QueryResult result) {
		QueryResult expected = temporalValuesToQueryResult(List.of(TEMPORAL_VALUES_LIST_1.get(0)));

		assertEquals(expected, result);
	}
	// endregion

	// region SelectTemporalValuesByTime
	@Test
	public void testSelectTemporalValuesByTime() {
		QueryResult result = query("""
			SELECT *
			FROM temporal_values
			WHERE timeOfBirth > '09:27:21'
			""");

		assertSelectTemporalValuesByTime(result);
	}

	@Test
	public void testSelectTemporalValuesByTimePrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM temporal_values
				WHERE timeOfBirth > ?
				""",
			stmt -> stmt.setObject(1, LocalTime.parse("09:27:21")));

		assertSelectTemporalValuesByTime(result);
	}

	private void assertSelectTemporalValuesByTime(QueryResult result) {
		QueryResult expected = temporalValuesToQueryResult(concat(List.of(TEMPORAL_VALUES_LIST_1.get(0)), TEMPORAL_VALUES_LIST_2));

		assertEquals(expected, result);
	}
	// endregion

	// region SelectTemporalValuesByTimeEquals
	@Test
	public void testSelectTemporalValuesByTimeEquals() {
		QueryResult result = query("""
			SELECT *
			FROM temporal_values
			WHERE timeOfBirth = '12:00:00'
			""");

		assertSelectTemporalValuesByTimeEquals(result);
	}

	@Test
	public void testSelectTemporalValuesByTimeEqualsPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM temporal_values
				WHERE timeOfBirth = ?
				""",
			stmt -> stmt.setObject(1, LocalTime.parse("12:00:00")));

		assertSelectTemporalValuesByTimeEquals(result);
	}

	private void assertSelectTemporalValuesByTimeEquals(QueryResult result) {
		QueryResult expected = temporalValuesToQueryResult(List.of(TEMPORAL_VALUES_LIST_1.get(0)));

		assertEquals(expected, result);
	}
	// endregion

	// region SelectTemporalValuesByDate
	@Test
	public void testSelectTemporalValuesByDate() {
		QueryResult result = query("""
			SELECT *
			FROM temporal_values
			WHERE dateOfBirth > '1985-01-01'
			""");

		assertSelectTemporalValuesByDate(result);
	}

	@Test
	public void testSelectTemporalValuesByDatePrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM temporal_values
				WHERE dateOfBirth > ?
				""",
			stmt -> stmt.setObject(1, LocalDate.parse("1985-01-01")));

		assertSelectTemporalValuesByDate(result);
	}

	private void assertSelectTemporalValuesByDate(QueryResult result) {
		QueryResult expected = temporalValuesToQueryResult(TEMPORAL_VALUES_LIST_1);

		assertEquals(expected, result);
	}
	// endregion

	// region SelectTemporalValuesByDateEquals
	@Test
	public void testSelectTemporalValuesByDateEquals() {
		QueryResult result = query("""
			SELECT *
			FROM temporal_values
			WHERE dateOfBirth = '2002-06-15'
			""");

		assertSelectTemporalValuesByDateEquals(result);
	}

	@Test
	public void testSelectTemporalValuesByDateEqualsPrepared() {
		QueryResult result = queryPrepared("""
				SELECT *
				FROM temporal_values
				WHERE dateOfBirth = ?
				""",
			stmt -> stmt.setObject(1, LocalDate.parse("2002-06-15")));

		assertSelectTemporalValuesByDateEquals(result);
	}

	private void assertSelectTemporalValuesByDateEquals(QueryResult result) {
		QueryResult expected = temporalValuesToQueryResult(List.of(TEMPORAL_VALUES_LIST_1.get(0)));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testJoinFilterByDate() {
		QueryResult result = query("""
			SELECT e.courseCode
			FROM enrollment e
			JOIN temporal_values t
			ON t.userId = e.studentId
			WHERE t.dateOfBirth = '2002-06-15'
			""");

		QueryResult expected = new QueryResult(List.of("courseCode"), List.of(new Object[]{"PX41"}, new Object[]{"AA01"}));

		assertEquals(expected, result);
	}

	@Test
	public void testJoinFilterByTime() {
		QueryResult result = query("""
			SELECT e.courseCode
			FROM enrollment e
			JOIN temporal_values t
			ON t.userId = e.studentId
			WHERE t.timeOfBirth = '12:00:00'
			""");

		QueryResult expected = new QueryResult(List.of("courseCode"), List.of(new Object[]{"PX41"}, new Object[]{"AA01"}));

		assertEquals(expected, result);
	}

	@Test
	public void testJoinFilterByTimestamp() {
		QueryResult result = query("""
			SELECT e.courseCode
			FROM enrollment e
			JOIN temporal_values t
			ON t.userId = e.studentId
			WHERE t.registeredAt = '2022-06-15 12:00:00'
			""");

		QueryResult expected = new QueryResult(List.of("courseCode"), List.of(new Object[]{"PX41"}, new Object[]{"AA01"}));

		assertEquals(expected, result);
	}

	// region SelectByStateEnum
	@Test
	public void testSelectByStateEnum() {
		QueryResult result = query("""
			SELECT id
			FROM states
			WHERE state = 'ON'
			""");

		assertSelectByStateEnum(result);
	}

	@Test
	public void testSelectByStateEnumPrepared() {
		QueryResult result = queryPrepared("""
			SELECT id
			FROM states
			WHERE state = ?
			""", stmt -> stmt.setString(1, "ON"));

		assertSelectByStateEnum(result);
	}

	private static void assertSelectByStateEnum(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"), List.of(new Object[]{1}, new Object[]{4}));

		assertEquals(expected, result);
	}
	// endregion

	// region SelectByInnerStateEnum
	@Test
	public void testSelectByInnerStateEnum() {
		QueryResult result = query("""
			SELECT id
			FROM states
			WHERE states.innerState.state = 'OFF'
			""");

		assertSelectByInnerStateEnum(result);
	}

	@Test
	public void testSelectByInnerStateEnumPrepared() {
		QueryResult result = queryPrepared("""
			SELECT id
			FROM states
			WHERE states.innerState.state = ?
			""", stmt -> stmt.setString(1, "OFF"));

		assertSelectByInnerStateEnum(result);
	}

	private static void assertSelectByInnerStateEnum(QueryResult result) {
		QueryResult expected = new QueryResult(List.of("id"), List.of(new Object[]{1}, new Object[]{3}));

		assertEquals(expected, result);
	}
	// endregion

	@Test
	public void testSortByEnum() {
		QueryResult result = query("""
			SELECT id
			FROM states
			ORDER BY state ASC, states.innerState.state DESC
			""");

		QueryResult expected = new QueryResult(
			List.of("id"),
			List.of(new Object[]{2}, new Object[]{3}, new Object[]{4}, new Object[]{1})
		);

		assertTrue(expected.equalsOrdered(result));
	}

	protected abstract QueryResult query(String sql);

	protected abstract QueryResult queryPrepared(String sql, ParamsSetter paramsSetter);

	protected abstract List<QueryResult> queryPreparedRepeated(String sql, ParamsSetter... paramsSetters);

	protected Object wrapLocalDateTime(LocalDateTime localDateTime) {
		return localDateTime;
	}

	public static final class QueryResult {
		private static final QueryResult EMPTY = new QueryResult(Collections.emptyList(), Collections.emptyList());

		private final List<String> columnNames;
		private final List<Object[]> columnValues;

		public QueryResult(List<String> columnNames, List<Object[]> columnValues) {
			if (!columnValues.isEmpty()) {
				int size = columnNames.size();
				for (Object[] columnValue : columnValues) {
					assertEquals(size, columnValue.length);
				}
			}

			this.columnNames = columnNames;
			this.columnValues = new ArrayList<>(columnValues.size());

			for (Object[] columnValue : columnValues) {
				Object[] newColumnValue = null;

				for (int i = 0; i < columnValue.length; i++) {
					Object o = columnValue[i];
					if (o instanceof Enum<?> anEnum) {
						// JDBC returns enums as Strings
						if (newColumnValue == null) {
							newColumnValue = Arrays.copyOf(columnValue, columnValue.length);
						}
						newColumnValue[i] = anEnum.name();
					}
				}

				this.columnValues.add(newColumnValue == null ? columnValue : newColumnValue);
			}
		}

		public static QueryResult empty() {
			return EMPTY;
		}

		public boolean isEmpty() {
			return columnValues.isEmpty();
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private static final Comparator<Object[]> VALUES_COMPARATOR = (values1, values2) -> {
			for (int i = 0; i < values1.length; i++) {
				Object value1 = values1[i];
				Object value2 = values2[i];

				if (value1 == value2) continue;
				if (value1 == null) return -1;
				if (value2 == null) return 1;

				Comparable comparable1 = (Comparable) value1;
				Comparable comparable2 = (Comparable) value2;
				Comparator<Comparable> naturalOrder = Comparator.naturalOrder();
				int result = naturalOrder.compare(comparable1, comparable2);
				if (result == 0) continue;
				return result;
			}
			return 0;
		};

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			QueryResult that = (QueryResult) o;

			if (!columnNames.equals(that.columnNames)) return false;

			if (columnValues.size() != that.columnValues.size()) return false;

			List<Object[]> sortedValues = columnValues.stream().sorted(VALUES_COMPARATOR).toList();
			List<Object[]> thatSortedValues = that.columnValues.stream().sorted(VALUES_COMPARATOR).toList();

			for (int i = 0; i < sortedValues.size(); i++) {
				Object[] columnValue = sortedValues.get(i);
				Object[] thatColumnValue = thatSortedValues.get(i);

				if (doesArraysDiffer(columnValue, thatColumnValue)) return false;
			}

			return true;
		}

		public boolean equalsOrdered(QueryResult that) {
			if (this == that) return true;
			if (that == null) return false;

			if (!columnNames.equals(that.columnNames)) return false;

			if (columnValues.size() != that.columnValues.size()) return false;

			for (int i = 0; i < columnValues.size(); i++) {
				Object[] columnValue = columnValues.get(i);
				Object[] thatColumnValue = that.columnValues.get(i);

				if (doesArraysDiffer(columnValue, thatColumnValue)) return false;
			}

			return true;
		}

		private static boolean doesArraysDiffer(Object[] columnValue, Object[] thatColumnValue) {
			if (columnValue == thatColumnValue)
				return false;
			if (columnValue == null || thatColumnValue == null)
				return true;

			int length = columnValue.length;
			if (thatColumnValue.length != length)
				return true;

			for (int i = 0; i < length; i++) {
				Object value = columnValue[i];
				Object thatValue = thatColumnValue[i];

				if (value instanceof Set<?> && thatValue instanceof List<?> thatList) {
					thatValue = new HashSet<>(thatList);
				} else if (thatValue instanceof Set<?> && value instanceof List<?> list) {
					value = new HashSet<>(list);
				}

				try {
					if (value instanceof Array array && thatValue instanceof List<?>) {
						value = List.of((Object[]) array.getArray());
					} else if (thatValue instanceof Array array && value instanceof List<?>) {
						thatValue = List.of((Object[]) array.getArray());
					}
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}

				if (!Objects.equals(value, thatValue)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public String toString() {
			return
				"QueryResult{" +
				"columnNames=" + columnNames +
				", columnValues=" + columnValues.stream()
					.map(Arrays::toString)
					.toList() +
				'}';
		}
	}

	private static QueryResult departmentsToQueryResult(List<Department> departments) {
		List<String> columnNames = Arrays.asList("id", "departmentName", "aliases");
		List<Object[]> columnValues = new ArrayList<>(departments.size());

		for (Department department : departments) {
			columnValues.add(new Object[]{department.id(), department.departmentName(), department.aliases()});
		}

		return new QueryResult(columnNames, columnValues);
	}

	private static QueryResult studentsToQueryResult(List<Student> students) {
		List<String> columnNames = Arrays.asList("id", "firstName", "lastName", "dept");
		List<Object[]> columnValues = new ArrayList<>(students.size());

		for (Student student : students) {
			columnValues.add(new Object[]{student.id(), student.firstName(), student.lastName(), student.dept()});
		}

		return new QueryResult(columnNames, columnValues);
	}

	private static QueryResult largeToQueryResult(List<Large> largeList) {
		List<String> columnNames = List.of("id");
		List<Object[]> columnValues = new ArrayList<>(largeList.size());

		for (Large large : largeList) {
			columnValues.add(new Object[]{large.id()});
		}

		return new QueryResult(columnNames, columnValues);
	}

	private QueryResult temporalValuesToQueryResult(List<TemporalValues> temporalValues) {
		List<String> columnNames = Arrays.asList("userId", "registeredAt", "dateOfBirth", "timeOfBirth");
		List<Object[]> columnValues = new ArrayList<>(temporalValues.size());

		for (TemporalValues temporalValue : temporalValues) {
			Object wrappedLocalDateTime = wrapLocalDateTime(temporalValue.registeredAt());
			columnValues.add(new Object[]{temporalValue.userId(), wrappedLocalDateTime, temporalValue.dateOfBirth(), temporalValue.timeOfBirth()});
		}

		return new QueryResult(columnNames, columnValues);
	}

	private static QueryResult registryToQueryResult(List<Registry> registries) {
		List<String> columnNames = Arrays.asList("id", "counters", "domains");
		List<Object[]> columnValues = new ArrayList<>(registries.size());

		for (Registry registry : registries) {
			columnValues.add(new Object[]{registry.id(), registry.counters(), registry.domains()});
		}

		return new QueryResult(columnNames, columnValues);
	}

	private static Student columnsToStudent(Object[] columnValue) {
		return new Student((Integer) columnValue[0], (String) columnValue[1], (String) columnValue[2], (Integer) columnValue[3]);
	}

	public interface ParamsSetter {
		void setValues(PreparedStatement stmt) throws SQLException;
	}

}
