using StudySystem.App;
using Xunit;

namespace StudySystem.Tests;

public sealed class StudySystemTests
{
    [Fact]
    public void UserFactoryCreatesStudent()
    {
        var user = new StudentFactory().Create("Eva", "eva@example.com");

        Assert.IsType<Student>(user);
    }

    [Fact]
    public void ObserverReceivesCourseChange()
    {
        var teacher = new Teacher(Guid.NewGuid(), "T", "t@example.com");
        var student = new Student(Guid.NewGuid(), "S", "s@example.com");
        var course = new StandardCourseFactory().Create("KI", "Kurz", teacher);

        course.Enroll(student);
        course.UpdateInfo("Novy kurz", teacher);

        Assert.Contains(student.Notifications, n => n.Contains("zmeneny"));
    }

    [Fact]
    public void WeightedStrategyCalculatesWeightedAverage()
    {
        var strategy = new WeightedAverageGradeStrategy();

        var result = strategy.Calculate([new Grade("A", 1, 1), new Grade("B", 3, 3)]);

        Assert.Equal(2.5, result, precision: 3);
    }
}
