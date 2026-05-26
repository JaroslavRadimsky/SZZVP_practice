namespace StudySystem.App;

public abstract record User(Guid Id, string Name, string Email);
public sealed record Student(Guid Id, string Name, string Email) : User(Id, Name, Email), ICourseObserver
{
    private readonly List<string> _notifications = [];
    public IReadOnlyList<string> Notifications => _notifications;
    public void Notify(Course course, string message) => _notifications.Add($"{course.Code}: {message}");
}

public sealed record Teacher(Guid Id, string Name, string Email) : User(Id, Name, Email);
public sealed record Admin(Guid Id, string Name, string Email) : User(Id, Name, Email);

/// <summary>Factory Method for creating account types.</summary>
public abstract class UserFactory
{
    public abstract User Create(string name, string email);
}

public sealed class StudentFactory : UserFactory
{
    public override User Create(string name, string email) => new Student(Guid.NewGuid(), name, email);
}

public sealed class TeacherFactory : UserFactory
{
    public override User Create(string name, string email) => new Teacher(Guid.NewGuid(), name, email);
}

public sealed class AdminFactory : UserFactory
{
    public override User Create(string name, string email) => new Admin(Guid.NewGuid(), name, email);
}

public interface ICourseObserver
{
    void Notify(Course course, string message);
}

public sealed record Grade(string Title, double Value, double Weight = 1);

public interface IGradeStrategy
{
    double Calculate(IEnumerable<Grade> grades);
}

public sealed class AverageGradeStrategy : IGradeStrategy
{
    public double Calculate(IEnumerable<Grade> grades)
    {
        var list = grades.ToList();
        return list.Count == 0 ? 0 : list.Average(g => g.Value);
    }
}

public sealed class WeightedAverageGradeStrategy : IGradeStrategy
{
    public double Calculate(IEnumerable<Grade> grades)
    {
        var list = grades.ToList();
        var weight = list.Sum(g => g.Weight);
        return weight <= 0 ? 0 : list.Sum(g => g.Value * g.Weight) / weight;
    }
}

public sealed class Course
{
    private readonly List<ICourseObserver> _observers = [];
    private readonly Dictionary<Guid, List<Grade>> _grades = [];

    public Course(string code, string title, Teacher teacher, IGradeStrategy strategy)
    {
        Code = code;
        Title = title;
        Teacher = teacher;
        Strategy = strategy;
    }

    public string Code { get; }
    public string Title { get; private set; }
    public Teacher Teacher { get; private set; }
    public IGradeStrategy Strategy { get; set; }
    public IReadOnlyCollection<Guid> EnrolledStudentIds => _grades.Keys.ToList();

    public void Enroll(Student student)
    {
        _grades.TryAdd(student.Id, []);
        if (!_observers.Contains(student))
        {
            _observers.Add(student);
        }
        Notify($"Student {student.Name} byl prihlasen.");
    }

    public void UpdateInfo(string title, Teacher teacher)
    {
        Title = title;
        Teacher = teacher;
        Notify("Informace o kurzu byly zmeneny.");
    }

    public void AddGrade(Student student, Grade grade)
    {
        if (!_grades.ContainsKey(student.Id))
        {
            throw new InvalidOperationException("Student neni zapsan v kurzu.");
        }
        _grades[student.Id].Add(grade);
        student.Notify(this, $"Pridana znamka {grade.Title}: {grade.Value}");
    }

    public double ResultFor(Student student) => _grades.TryGetValue(student.Id, out var grades) ? Strategy.Calculate(grades) : 0;

    private void Notify(string message)
    {
        foreach (var observer in _observers)
        {
            observer.Notify(this, message);
        }
    }
}

/// <summary>Factory Method for course creation with a default evaluation strategy.</summary>
public abstract class CourseFactory
{
    public abstract Course Create(string code, string title, Teacher teacher);
}

public sealed class StandardCourseFactory : CourseFactory
{
    public override Course Create(string code, string title, Teacher teacher) =>
        new(code, title, teacher, new AverageGradeStrategy());
}

public sealed class WeightedCourseFactory : CourseFactory
{
    public override Course Create(string code, string title, Teacher teacher) =>
        new(code, title, teacher, new WeightedAverageGradeStrategy());
}

public sealed class StudySystemService
{
    private readonly List<User> _users = [];
    private readonly List<Course> _courses = [];

    public IReadOnlyList<User> Users => _users;
    public IReadOnlyList<Course> Courses => _courses;

    public User Register(UserFactory factory, string name, string email)
    {
        var user = factory.Create(name, email);
        _users.Add(user);
        return user;
    }

    public Course AddCourse(CourseFactory factory, string code, string title, Teacher teacher)
    {
        var course = factory.Create(code, title, teacher);
        _courses.Add(course);
        return course;
    }

    public Course? FindCourse(string code) => _courses.FirstOrDefault(c => c.Code.Equals(code, StringComparison.OrdinalIgnoreCase));
    public Student? FindStudent(string email) => _users.OfType<Student>().FirstOrDefault(s => s.Email.Equals(email, StringComparison.OrdinalIgnoreCase));
    public Teacher? FindTeacher(string email) => _users.OfType<Teacher>().FirstOrDefault(t => t.Email.Equals(email, StringComparison.OrdinalIgnoreCase));
}

public static class Program
{
    public static void Main()
    {
        var system = Seed();

        while (true)
        {
            Console.WriteLine();
            Console.WriteLine("Studijni system");
            Console.WriteLine("1 - Registrovat studenta");
            Console.WriteLine("2 - Registrovat ucitele");
            Console.WriteLine("3 - Vytvorit kurz");
            Console.WriteLine("4 - Zapsat studenta do kurzu");
            Console.WriteLine("5 - Pridat znamku");
            Console.WriteLine("6 - Zobrazit vysledky");
            Console.WriteLine("7 - Zobrazit oznameni studenta");
            Console.WriteLine("0 - Konec");
            Console.Write("Volba: ");

            try
            {
                switch (Console.ReadLine())
                {
                    case "1":
                        Register(system, new StudentFactory());
                        break;
                    case "2":
                        Register(system, new TeacherFactory());
                        break;
                    case "3":
                        CreateCourse(system);
                        break;
                    case "4":
                        Enroll(system);
                        break;
                    case "5":
                        AddGrade(system);
                        break;
                    case "6":
                        PrintResults(system);
                        break;
                    case "7":
                        PrintNotifications(system);
                        break;
                    case "0":
                        return;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Chyba: {ex.Message}");
            }
        }
    }

    private static StudySystemService Seed()
    {
        var system = new StudySystemService();
        var teacher = (Teacher)system.Register(new TeacherFactory(), "Petr Ucitel", "petr@ujep.cz");
        var student = (Student)system.Register(new StudentFactory(), "Jana Studentka", "jana@ujep.cz");
        var course = system.AddCourse(new StandardCourseFactory(), "KI-SWI", "Softwarove inzenyrstvi", teacher);
        course.Enroll(student);
        return system;
    }

    private static void Register(StudySystemService system, UserFactory factory)
    {
        var user = system.Register(factory, ReadText("Jmeno"), ReadText("E-mail"));
        Console.WriteLine($"Vytvoren ucet {user.GetType().Name}: {user.Name}");
    }

    private static void CreateCourse(StudySystemService system)
    {
        var teacher = system.FindTeacher(ReadText("E-mail ucitele")) ?? throw new InvalidOperationException("Ucitel nenalezen.");
        Console.Write("Vazeny prumer? a/n: ");
        CourseFactory factory = Console.ReadLine()?.Equals("a", StringComparison.OrdinalIgnoreCase) == true
            ? new WeightedCourseFactory()
            : new StandardCourseFactory();
        var course = system.AddCourse(factory, ReadText("Kod"), ReadText("Nazev"), teacher);
        Console.WriteLine($"Kurz {course.Code} vytvoren.");
    }

    private static void Enroll(StudySystemService system)
    {
        var course = system.FindCourse(ReadText("Kod kurzu")) ?? throw new InvalidOperationException("Kurz nenalezen.");
        var student = system.FindStudent(ReadText("E-mail studenta")) ?? throw new InvalidOperationException("Student nenalezen.");
        course.Enroll(student);
    }

    private static void AddGrade(StudySystemService system)
    {
        var course = system.FindCourse(ReadText("Kod kurzu")) ?? throw new InvalidOperationException("Kurz nenalezen.");
        var student = system.FindStudent(ReadText("E-mail studenta")) ?? throw new InvalidOperationException("Student nenalezen.");
        course.AddGrade(student, new Grade(ReadText("Nazev znamky"), ReadNumber("Hodnota"), ReadNumber("Vaha")));
    }

    private static void PrintResults(StudySystemService system)
    {
        foreach (var course in system.Courses)
        {
            foreach (var id in course.EnrolledStudentIds)
            {
                var student = system.Users.OfType<Student>().Single(s => s.Id == id);
                Console.WriteLine($"{course.Code} | {student.Name} | prumer {course.ResultFor(student):0.00}");
            }
        }
    }

    private static void PrintNotifications(StudySystemService system)
    {
        var student = system.FindStudent(ReadText("E-mail studenta")) ?? throw new InvalidOperationException("Student nenalezen.");
        foreach (var item in student.Notifications)
        {
            Console.WriteLine(item);
        }
    }

    private static string ReadText(string label)
    {
        Console.Write($"{label}: ");
        return Console.ReadLine()?.Trim() ?? "";
    }

    private static double ReadNumber(string label)
    {
        Console.Write($"{label}: ");
        return double.TryParse(Console.ReadLine(), out var value) ? value : 0;
    }
}

