import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * College Attendance Management System - Batch No. 5
 * Students: 25881A05DE, 25881A05EA, 25881A05CU
 *
 * A complete menu-driven console application written in a SINGLE file.
 * OOP concepts demonstrated:
 *   Encapsulation  - private fields with getters/setters
 *   Inheritance    - Student and Teacher extend abstract Person
 *   Polymorphism   - overridden displayInfo(), overloaded markAttendance()
 *   Abstraction    - abstract Person class + Reportable interface
 *   Aggregation    - AttendanceManager HAS-A list of Students, Subjects, Records
 *   Exception handling - custom exception + try-catch for bad input
 *
 * Compile: javac CollegeAttendanceSystem.java
 * Run:     java CollegeAttendanceSystem
 */
public class CollegeAttendanceSystem {

    // ================= ABSTRACTION =================
    // Abstract class: common features of every person in the college.
    // displayInfo() is abstract, so every subclass MUST give its own version
    // (this is runtime polymorphism).
    static abstract class Person {
        private String id;    // roll number for students, faculty id for teachers
        private String name;

        public Person(String id, String name) {
            this.id = id;
            this.name = name;
        }

        // ---- Encapsulation: controlled access through getters/setters ----
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public abstract void displayInfo(); // implemented differently by each subclass
    }

    // ================= INHERITANCE =================
    static class Student extends Person {
        private String branch;
        private int semester;

        public Student(String rollNo, String name, String branch, int semester) {
            super(rollNo, name); // constructor chaining to parent class
            this.branch = branch;
            this.semester = semester;
        }

        public String getRollNo() { return getId(); }
        public String getBranch() { return branch; }
        public int getSemester() { return semester; }

        @Override
        public void displayInfo() {
            System.out.printf("Student | Roll: %-14s | Name: %-20s | %s, Sem %d%n",
                    getRollNo(), getName(), branch, semester);
        }
    }

    static class Teacher extends Person {
        private String department;

        public Teacher(String facultyId, String name, String department) {
            super(facultyId, name);
            this.department = department;
        }

        public String getDepartment() { return department; }

        @Override
        public void displayInfo() {
            System.out.printf("Teacher | ID: %-14s | Name: %-20s | Dept: %s%n",
                    getId(), getName(), department);
        }
    }

    // ================= SUBJECT =================
    static class Subject {
        private String code;
        private String title;
        private String teacherId;

        public Subject(String code, String title, String teacherId) {
            this.code = code;
            this.title = title;
            this.teacherId = teacherId;
        }

        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getTeacherId() { return teacherId; }

        public void displayInfo() {
            System.out.printf("Subject | Code: %-8s | Title: %-25s | Faculty: %s%n",
                    code, title, teacherId);
        }
    }

    // ================= ATTENDANCE RECORD =================
    enum Status { PRESENT, ABSENT, LATE }

    static class AttendanceRecord {
        private String rollNo;
        private String subjectCode;
        private LocalDate date;
        private Status status;

        public AttendanceRecord(String rollNo, String subjectCode, LocalDate date, Status status) {
            this.rollNo = rollNo;
            this.subjectCode = subjectCode;
            this.date = date;
            this.status = status;
        }

        public String getRollNo() { return rollNo; }
        public String getSubjectCode() { return subjectCode; }
        public LocalDate getDate() { return date; }
        public Status getStatus() { return status; }

        public boolean isAttended() {
            return status == Status.PRESENT || status == Status.LATE;
        }
    }

    // ================= CUSTOM EXCEPTION =================
    static class RecordNotFoundException extends Exception {
        public RecordNotFoundException(String message) {
            super(message);
        }
    }

    // ================= INTERFACE (ABSTRACTION) =================
    interface Reportable {
        void generateStudentReport(String rollNo) throws RecordNotFoundException;
        void generateDefaulterReport(double thresholdPercentage);
    }

    // ================= MANAGER (AGGREGATION + BUSINESS LOGIC) =================
    static class AttendanceManager implements Reportable {
        private List<Student> students = new ArrayList<>();
        private List<Teacher> teachers = new ArrayList<>();
        private List<Subject> subjects = new ArrayList<>();
        private List<AttendanceRecord> records = new ArrayList<>();
        private Map<String, Subject> subjectByCode = new HashMap<>();

        // ---- Add operations ----
        public boolean addStudent(Student s) {
            if (findStudent(s.getRollNo()) != null) return false; // no duplicates
            students.add(s);
            return true;
        }

        public void addTeacher(Teacher t) { teachers.add(t); }

        public boolean addSubject(Subject s) {
            if (subjectByCode.containsKey(s.getCode())) return false;
            subjects.add(s);
            subjectByCode.put(s.getCode(), s);
            return true;
        }

        public Student findStudent(String rollNo) {
            for (Student s : students) {
                if (s.getRollNo().equalsIgnoreCase(rollNo)) return s;
            }
            return null;
        }

        public Subject findSubject(String code) {
            return subjectByCode.get(code.toUpperCase());
        }

        // ---- Method overloading: same name, different parameters ----
        public void markAttendance(String rollNo, String subjectCode, Status status) {
            markAttendance(rollNo, subjectCode, LocalDate.now(), status);
        }

        public void markAttendance(String rollNo, String subjectCode, LocalDate date, Status status) {
            // if already marked for this student+subject+date, replace the old entry
            records.removeIf(r -> r.getRollNo().equalsIgnoreCase(rollNo)
                    && r.getSubjectCode().equalsIgnoreCase(subjectCode)
                    && r.getDate().equals(date));
            records.add(new AttendanceRecord(rollNo, subjectCode.toUpperCase(), date, status));
        }

        public List<AttendanceRecord> recordsOf(String rollNo) {
            List<AttendanceRecord> out = new ArrayList<>();
            for (AttendanceRecord r : records) {
                if (r.getRollNo().equalsIgnoreCase(rollNo)) out.add(r);
            }
            return out;
        }

        public double attendancePercentage(String rollNo) {
            List<AttendanceRecord> list = recordsOf(rollNo);
            if (list.isEmpty()) return -1; // no classes conducted yet
            long attended = 0;
            for (AttendanceRecord r : list) {
                if (r.isAttended()) attended++;
            }
            return attended * 100.0 / list.size();
        }

        // ---- Interface implementation: student report ----
        @Override
        public void generateStudentReport(String rollNo) throws RecordNotFoundException {
            Student s = findStudent(rollNo);
            if (s == null) throw new RecordNotFoundException("No student with roll number " + rollNo);
            List<AttendanceRecord> list = recordsOf(rollNo);
            System.out.println("\n========== ATTENDANCE REPORT ==========");
            s.displayInfo();
            if (list.isEmpty()) {
                System.out.println("No attendance records yet.");
                return;
            }
            System.out.println("-------------------------------------------------");
            System.out.printf("%-12s %-10s %-12s %-8s%n", "Date", "Subject", "Status", "");
            for (AttendanceRecord r : list) {
                System.out.printf("%-12s %-10s %-12s%n", r.getDate(), r.getSubjectCode(), r.getStatus());
            }
            System.out.println("-------------------------------------------------");
            long present = 0, absent = 0, late = 0;
            for (AttendanceRecord r : list) {
                switch (r.getStatus()) {
                    case PRESENT: present++; break;
                    case ABSENT: absent++; break;
                    case LATE: late++; break;
                }
            }
            System.out.printf("Total classes: %d | Present: %d | Late: %d | Absent: %d%n",
                    list.size(), present, late, absent);
            System.out.printf("Attendance Percentage: %.2f%%%n", attendancePercentage(rollNo));
        }

        // ---- Interface implementation: subject-wise report ----
        public void generateSubjectReport(String code) throws RecordNotFoundException {
            Subject sub = findSubject(code);
            if (sub == null) throw new RecordNotFoundException("No subject with code " + code);
            System.out.println("\n========== SUBJECT REPORT ==========");
            sub.displayInfo();
            System.out.println("-------------------------------------------------");
            boolean any = false;
            for (Student s : students) {
                long total = 0, attended = 0;
                for (AttendanceRecord r : records) {
                    if (r.getRollNo().equalsIgnoreCase(s.getRollNo())
                            && r.getSubjectCode().equalsIgnoreCase(code)) {
                        total++;
                        if (r.isAttended()) attended++;
                    }
                }
                if (total > 0) {
                    any = true;
                    System.out.printf("%-14s %-20s %d/%d  (%.2f%%)%n",
                            s.getRollNo(), s.getName(), attended, total, attended * 100.0 / total);
                }
            }
            if (!any) System.out.println("No attendance marked for this subject yet.");
        }

        // ---- Interface implementation: defaulters ----
        @Override
        public void generateDefaulterReport(double thresholdPercentage) {
            System.out.println("\n========== DEFAULTERS (below " + thresholdPercentage + "%) ==========");
            boolean any = false;
            for (Student s : students) {
                double pct = attendancePercentage(s.getRollNo());
                if (pct >= 0 && pct < thresholdPercentage) {
                    any = true;
                    System.out.printf("%-14s %-20s %.2f%%%n", s.getRollNo(), s.getName(), pct);
                }
            }
            if (!any) System.out.println("No defaulters. Good!");
        }

        // ---- Polymorphism demo: one list, two behaviours ----
        public void showAllPeople() {
            System.out.println("\n========== ALL PEOPLE (Polymorphism Demo) ==========");
            List<Person> people = new ArrayList<>();
            people.addAll(students);
            people.addAll(teachers);
            for (Person p : people) {
                p.displayInfo(); // Student or Teacher version runs automatically
            }
        }

        public void showAllStudents() {
            System.out.println("\n========== ALL STUDENTS ==========");
            for (Student s : students) s.displayInfo();
        }

        public void showAllSubjects() {
            System.out.println("\n========== ALL SUBJECTS ==========");
            for (Subject s : subjects) s.displayInfo();
        }

        public List<Student> getStudents() { return students; }
    }

    // ================= SAMPLE DATA (Batch 5 team + demo classes) =================
    private static void loadSampleData(AttendanceManager m) {
        m.addTeacher(new Teacher("T001", "Mr. M. Raghavendra", "CSE"));
        m.addStudent(new Student("25881A05DE", "Student DE", "CSE", 3));
        m.addStudent(new Student("25881A05EA", "Student EA", "CSE", 3));
        m.addStudent(new Student("25881A05CU", "Student CU", "CSE", 3));
        m.addSubject(new Subject("CS201", "Java Programming", "T001"));
        m.addSubject(new Subject("CS202", "Database Management Systems", "T001"));
        m.addSubject(new Subject("CS203", "Operating Systems", "T001"));
        // a few demo attendance entries so reports work immediately
        m.markAttendance("25881A05DE", "CS201", LocalDate.of(2026, 10, 26), Status.PRESENT);
        m.markAttendance("25881A05EA", "CS201", LocalDate.of(2026, 10, 26), Status.PRESENT);
        m.markAttendance("25881A05CU", "CS201", LocalDate.of(2026, 10, 26), Status.ABSENT);
        m.markAttendance("25881A05DE", "CS201", LocalDate.of(2026, 10, 27), Status.PRESENT);
        m.markAttendance("25881A05EA", "CS201", LocalDate.of(2026, 10, 27), Status.LATE);
        m.markAttendance("25881A05CU", "CS201", LocalDate.of(2026, 10, 27), Status.PRESENT);
        m.markAttendance("25881A05EA", "CS202", LocalDate.of(2026, 10, 27), Status.PRESENT);
    }

    // ================= CONSOLE UI =================
    public static void main(String[] args) {
        AttendanceManager manager = new AttendanceManager();
        loadSampleData(manager);
        Scanner sc = new Scanner(System.in);

        System.out.println("==============================================");
        System.out.println("  COLLEGE ATTENDANCE MANAGEMENT SYSTEM");
        System.out.println("  Batch 5: 25881A05DE | 25881A05EA | 25881A05CU");
        System.out.println("==============================================");

        while (true) {
            System.out.println("\n----- MENU -----");
            System.out.println("1. Add Student");
            System.out.println("2. Add Subject");
            System.out.println("3. Mark Attendance (whole class, one subject)");
            System.out.println("4. Student Attendance Report");
            System.out.println("5. Subject-wise Report");
            System.out.println("6. Defaulters List (below 75%)");
            System.out.println("7. View All Students");
            System.out.println("8. View All Subjects");
            System.out.println("9. View All People (Polymorphism Demo)");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number from the menu.");
                continue;
            }

            try {
                switch (choice) {
                    case 1: {
                        System.out.print("Roll number: ");
                        String roll = sc.nextLine().trim();
                        System.out.print("Name: ");
                        String name = sc.nextLine().trim();
                        System.out.print("Branch: ");
                        String branch = sc.nextLine().trim();
                        System.out.print("Semester: ");
                        int sem = Integer.parseInt(sc.nextLine().trim());
                        if (manager.addStudent(new Student(roll, name, branch, sem)))
                            System.out.println("Student added.");
                        else
                            System.out.println("Roll number already exists!");
                        break;
                    }
                    case 2: {
                        System.out.print("Subject code: ");
                        String code = sc.nextLine().trim().toUpperCase();
                        System.out.print("Subject title: ");
                        String title = sc.nextLine().trim();
                        System.out.print("Faculty ID: ");
                        String fid = sc.nextLine().trim();
                        if (manager.addSubject(new Subject(code, title, fid)))
                            System.out.println("Subject added.");
                        else
                            System.out.println("Subject code already exists!");
                        break;
                    }
                    case 3: {
                        System.out.print("Subject code: ");
                        String code = sc.nextLine().trim();
                        if (manager.findSubject(code) == null) {
                            System.out.println("Subject not found!");
                            break;
                        }
                        System.out.print("Date (YYYY-MM-DD, Enter = today): ");
                        String dateStr = sc.nextLine().trim();
                        LocalDate date = dateStr.isEmpty()
                                ? LocalDate.now() : LocalDate.parse(dateStr);
                        for (Student s : manager.getStudents()) {
                            System.out.printf("%s (%s) - P/A/L: ", s.getName(), s.getRollNo());
                            String in = sc.nextLine().trim().toUpperCase();
                            Status st = in.equals("A") ? Status.ABSENT
                                    : in.equals("L") ? Status.LATE : Status.PRESENT;
                            manager.markAttendance(s.getRollNo(), code, date, st);
                        }
                        System.out.println("Attendance marked for " + date);
                        break;
                    }
                    case 4: {
                        System.out.print("Enter roll number: ");
                        manager.generateStudentReport(sc.nextLine().trim());
                        break;
                    }
                    case 5: {
                        System.out.print("Enter subject code: ");
                        manager.generateSubjectReport(sc.nextLine().trim());
                        break;
                    }
                    case 6:
                        manager.generateDefaulterReport(75.0);
                        break;
                    case 7:
                        manager.showAllStudents();
                        break;
                    case 8:
                        manager.showAllSubjects();
                        break;
                    case 9:
                        manager.showAllPeople();
                        break;
                    case 0:
                        System.out.println("Thank you! Exiting...");
                        sc.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number entered. Try again.");
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
            } catch (RecordNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
