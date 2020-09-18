// Copyright(C) 2019 - John A. De Goes. All rights reserved.
// Answers by James McCabe (jjtmcabe@gmail.com)

package net.degoes.zio
package essentials

import java.io.{File, IOException}
import java.util.concurrent.{Executors, TimeUnit}

import net.degoes.zio.essentials.zio_environment.FileSystem.Service
import zio._

import scala.language.postfixOps
import scala.io.Source
import zio.internal.PlatformLive

import scala.annotation.tailrec

/**
 * `ZIO[R, E, A]` is an immutable data structure that models an effect, which
 * could be synchronous, asynchronous, concurrent, resourceful, errorful, or
 * environmental.
 *
 *  - The effect requires an environment `R`
 *  - The effect may fail with an error `E`
 *  - The effect may succeed with a value `A`
 *
 * Mental model: An effectful version of: `R => Either[E, A]`
 */
object zio_types {

  /**
   * Write the following types in terms of the `ZIO` type.
   */
  /**
   * EXERCISE 1
   *
   * An effect that might fail with an error of type `E` or succeed with a
   * value of type `A`.
   */
  type FailOrSuccess[E, A] = ZIO[Any, E, A]

  /**
   * EXERCISE 2
   *
   * An effect that never fails and might succeed with a value of type `A`
   */
  type Success[A] = ZIO[Any, Nothing, A]

  /**
   * EXERCISE 3
   *
   * An effect that runs forever but might fail with `E`.
   */
  type Forever[E] = ZIO[Any, E, Nothing]

  /**
   * EXERCISE 4
   *
   * An effect that cannot fail or succeed with a value.
   */
  type NeverStops = ZIO[Any, Nothing, Nothing]

  /**
   * EXERCISE 5
   *
   * An effect that may fail with a value of type `E` or succeed with a value
   * of type `A`, and doesn't require any specific environment.
   */
  type IO[+E, +A] = ZIO[Any, E, A]

  /**
   * EXERCISE 6
   *
   * An effect that may fail with `Throwable` or succeed with a value of
   * type `A`, and doesn't require any specific environment.
   */
  type Task[+A] = ZIO[Any, Throwable, A]

  /**
   * EXERCISE 7
   *
   * An effect that cannot fail but may succeed with a value of type `A`,
   * and doesn't require any specific environment.
   */
  type UIO[+A] = ZIO[Any, Nothing, A]

  /**
   * EXERCISE 8
   *
   * An effect that may fail with `Throwable` or succeed with a value of
   * type `A`, and which requires an `R` environment.
   */
  type RIO[-R, +A] = ZIO[R, Throwable, A]

  /**
   * EXERCISE 8
   *
   * An effect that cannot fail, but may succeed with a value of
   * type `A`, and which requires an `R` environment.
   */
  type URIO[-R, +A] = ZIO[R, Nothing, A]
}

object zio_values {

  /**
   * EXERCISE 1
   *
   * Using the `ZIO.succeed` method. Construct an effect that succeeds with the
   * integer `42`, and ascribe the correct type.
   */
  val ioInt: UIO[Int] = ZIO.succeed(42)

  /**
   * EXERCISE 2
   *
   * Using the `ZIO.fail` method, construct an effect that fails with the string
   * "Incorrect value", and ascribe the correct type.
   */
  val incorrectVal: IO[String, Nothing] = ZIO.fail("Incorrect value")

  /**
   * EXERCISE 3
   *
   * Using the `ZIO.effectTotal` method, construct an effect that wraps Scala
   * `println` method, so you have a pure functional version of `println`, and
   * ascribe the correct type.
   */
  def putStrLn(line: String): UIO[Unit] = ZIO.effectTotal(println(line))

  /**
   * EXERCISE 4
   *
   * Using the `ZIO.effect` method, wrap Scala's `readLine` method to make it
   * purely functional with the correct ZIO error type.
   *
   * Note: You will have to use the `.refineOrDie` method to refine the
   * `Throwable` type into something more specific.
   */
  val getStrLn: Task[String] = ZIO.effect(scala.io.StdIn.readLine).refineToOrDie[IOException]

  /**
   * EXERCISE 6
   *
   * Using the `ZIO.effect` method, wrap Scala's `getLines` to make it
   * purely functional with the correct ZIO error type.
   *
   * Note: You will have to use the `.refineOrDie` method to refine the
   * `Throwable` type into `IOException`.
   */
  def readFile(file: File): IO[IOException, List[String]] =
    ZIO.effect(Source.fromFile(file).getLines.toList).refineToOrDie[IOException]

  /**
   * EXERCISE 7
   *
   * Using the `ZIO.effect` method, wrap Scala's `Array#update` method to make
   * it purely functional with the correct ZIO error type.
   *
   * Note: You will have to use the `.refineOrDie` method to refine the
   * `Throwable` type into something more specific.
   */
  def arrayUpdate[A](a: Array[A], i: Int, f: A => A): IO[ArrayIndexOutOfBoundsException, Unit] =
    ZIO.effect(a.update(i, f(a(i)))).refineToOrDie[ArrayIndexOutOfBoundsException]


  /**
   * EXERCISE 8
   *
   * Using the `ZIO#refineOrDie` method, catch the `NoSuchElementException` and
   * return -1.
   */
  def firstOrNegative1(as: List[Int]): ZIO[Any, Int, Int] = Task.effect(as.head).refineOrDie[Int] {
    case e: NoSuchElementException => -1
  }


  /**
   * EXERCISE 9
   *
   * Using the `ZIO.effectAsync` method, translate the `ScheduledExecutor` callback-
   * based API into a ZIO effect.
   */
  val scheduledExecutor = Executors.newScheduledThreadPool(1)
  def sleep(l: Long, u: TimeUnit): ZIO[Any, Nothing, Unit] =
    ZIO.effectAsync[Any, Nothing, Unit] { callback =>
      scheduledExecutor
        .schedule(new Runnable {
          def run(): Unit = callback(ZIO.unit)
        }, l, u)
    }

  /**
   * EXERCISE 10
   *
   * Using the `ZIO.effectAsync` method, translate the following callback-based API
   * into a ZIO API that does not use any callbacks.

   */
  def readChunkCB(success: Array[Byte] => Unit, failure: Throwable => Unit): Unit = ???

  val readChunkIO: Task[Array[Byte]]                                              =
    ZIO.effectAsync[Any, Throwable, Array[Byte]] { callback =>
      readChunkCB(
        success => callback(IO.succeed(success)),
        failure => callback(IO.fail(failure))
      )
    }

  /**
   * EXERCISE 11
   *
   * Using the `ZIO.effectAsyncInterrupt` method, wrap the following Java
   * callback API into a ZIO effect. When the ZIO effect is interrupted,
   * call the canceller in the returned `HttpGetToken` of the Java API.
   */
  // Note: the Canceler type is expected by ZIO.effectAsyncInterrupt, so a correction is made here
  //case class HttpGetToken(canceller: () => Unit) // removed
  case class HttpGetToken(canceller: Canceler[Any])

  def httpGetCB(url: String)(success: Array[Byte] => Unit, error: Throwable => Unit): HttpGetToken = ???
  def httpGetIO(url: String): Task[Array[Byte]]                                                    =
    ZIO.effectAsyncInterrupt[Any, Throwable, Array[Byte]] { register =>
      val httpGetToken = httpGetCB(url)(
        success => register(IO.succeed(success)),
        error => register(IO.fail(error))
      )
      Left(httpGetToken.canceller)
    }

  /**
   * EXERCISE 12
   *
   * In order to run effects, you need to interpret them using a `Runtime` in
   *  ZIO (such as `DefaultRuntime`) and call `unsafeRun`, or write your
   * pure main function inside `App`.
   */
  object Example extends DefaultRuntime {
    val sayHelloIO: UIO[Unit] = putStrLn("Hello ZIO!")

    //run sayHelloIO using `unsafeRun`
    val sayHello: Unit = unsafeRun(sayHelloIO)
  }

  /**
   * EXERCISE 13
   *
   * Write a simple hello world program.
   */
  object MyMain extends App {
    def run(args: List[String]) = putStrLn("Hello World").fold(_ => 1, _ => 0)
}

/**
 * Basic operations in ZIO.
 */
object zio_operations {

  /**
   * EXERCISE 1
   *
   * Using `ZIO#map`, map an effect that succeeds with an `Int` into one that
   * succeeds with a string.
   */
  val toStr: UIO[String] = IO.succeed(42).map(_.toString)

  /**
   * EXERCISE 2
   *
   * Using `ZIO#map`, map an effect that succeeds with an `Int` into one that
   * succeeds with one plus that integer.
   */
  def addOne(i: Int): UIO[Int] = IO.succeed(i).map(x => x + 1)

  /**
   * EXERCISE 3
   *
   * Using the `ZIO#mapError` method, map an effect that fails with an `Int`
   * into one that fails with a string.
   */
  val toFailedStr: IO[String, Nothing] =
    IO.fail(42).mapError(_.toString)

  /**
   * EXERCISE 3
   *
   * Using `ZIO#flatMap`, check the integer produced by an effect, and if it
   * is even, return `attack`, but if it is odd, return `retreat`.
   */
  val attack: UIO[Boolean]  = UIO.effectTotal(println("Attacking!")).as(true)
  val retreat: UIO[Boolean] = UIO.effectTotal(println("Retreating!")).as(false)
  val action: UIO[Boolean]  = UIO(42).flatMap(x => if (x % 2 == 0) attack else retreat)

  /**
   * EXERCISE 4
   *
   * Using `ZIO#flatMap` and `ZIO#map` compute the sum of the values produced
   * by the `int1` and `int2` effects.
   */
  val int1: UIO[Int] = IO.succeed(14)
  val int2: UIO[Int] = IO.succeed(16)
  val sum: UIO[Int]  = int1.flatMap(x => int2.map(_ + x))

  /**
   * EXERCISE 5
   *
   * Translate this procedure (which repeats an action `n` times) into a function
   * that returns an effect that repeats the input effect the specified number of
   * times (hint: use `ZIO#flatMap` or `ZIO#zipRight`).
   */
  def repeatN1(n: Int, action: () => Unit): Unit =
    if (n <= 0) ()
    else {
      action()
      repeatN1(n - 1, action)
    }

  def repeatN2[E](n: Int, action: IO[E, Unit]): IO[E, Unit] =
    if (n <= 0) ZIO.unit else action *> repeatN2(n - 1, action)
}

  /**
   * EXERCISE 6
   *
   * Translate this factorial function into its ZIO equivalent.
   */
  def factorial(n: Int): Int =
    if (n <= 1) 1
    else n * factorial(n - 1)
  def factorialIO(n: Int): UIO[Int] =
    if (n <= 1) ZIO.succeed(1)
    else factorialIO(n - 1).map(_ * n)

  /**
   * EXERCISE 7
   *
   * Write a new version of the factorial function, this one tail recursive.
   */
  @tailrec
  def factorialTailIO(n: Int, acc: Int = 1): UIO[Int] =
    if (n <= 1) ZIO.succeed(acc)
    else factorialTailIO(n - 1, n * acc)

  /**
   * EXERCISE 8
   *
   * Using `ZIO#zip`, combine the result of two effects into a tuple.
   */
  def toTuple[A, B](io1: UIO[A], io2: UIO[B]): UIO[(A, B)] =
    io1.zip(io2)

  /**
   * EXERCISE 9
   *
   * Using `ZIO#zipWith`, add the two values produced by the two effects.
   */
  val combine: UIO[Int] = UIO.succeed(2).zipWith(UIO.succeed(40))((a, b) => a + b)

  /**
   * EXERCISE 10
   *
   * Using `ZIO.foreach`, convert a list of integers into a List of String
   */
  def convert(l: List[Int]): UIO[List[String]] = ZIO.foreach(l)(i => UIO.succeed(i.toString))

  /**
   * EXERCISE 11
   *
   * Using `ZIO.collectAll`
   * evaluate a list of effects and collect the result into an IO of a list with their result
   */
  def collect(effects: List[UIO[Int]]): UIO[List[Int]] = ZIO.collectAll(effects)

  /**
   * EXERCISE 12
   *
   * Rewrite the following series of `flatMap`/`map` into a `for` comprehension.
   */
  val nameAsk: Task[String] =
    Task
      .effect(println("What is your name?"))
      .flatMap(
        _ =>
          Task.effect(scala.io.StdIn.readLine()).flatMap(name => Task.effect(println(s"Hello, $name")).map(_ => name))
      )

  val nameAsk1: Task[String] =
    for {
      _    <- Task.effect(println("What is your name?"))
      name <- Task.effect(scala.io.StdIn.readLine())
      _    <- Task.effect(s"Hello, $name")
    } yield name

  /**
   * EXERCISE 13
   *
   * Rewrite the following `for` comprehension into a series of `flatMap`/`map`.
   */
  val ageAsk: Task[Int] =
    for {
      _     <- Task.effect(println("What is your age?"))
      input <- Task.effect(scala.io.StdIn.readLine())
      age   <- Task.fromTry(scala.util.Try(input.toInt))
    } yield age

  val ageAsk1: Task[Int] =
    Task
      .effect(println("What is your age?"))
      .flatMap(_ =>
        Task.effect(scala.io.StdIn.readLine())
          .flatMap(age => Task.fromTry(scala.util.Try(age.toInt))
      ))

  /**
   * EXERCISE 14
   *
   * Translate the following procedural program into its ZIO equivalent.
   */
  def playGame1(): Unit = {
    val number = scala.util.Random.nextInt(5)
    println("Enter a number between 0 - 5: ")
    scala.util.Try(scala.io.StdIn.readLine().toInt).toOption match {
      case None =>
        println("You didn't enter an integer!")
        playGame1()
      case Some(guess) if guess == number =>
        println("You guessed right! The number was " + number)
      case _ =>
        println("You guessed wrong! The number was " + number)
    }
  }

  lazy val playGame2: UIO[Unit] = {
    val number = scala.util.Random.nextInt(5)
    val guess = for {
      input <- zio.console.putStrLn("Enter a number between 0 - 5: ") *> getStrLn
      _     <- scala.util.Try(input.toInt).toOption match {
        case None => putStrLn("You didn't enter an integer!") *> playGame2
        case Some(guess) if (guess == number) =>
          putStrLn(s"You guessed right! The number was $number")
        case _ =>
          putStrLn(s"You guessed wrong! The number was $number")
      }
    } yield ()
    ZIO.unit
  }
}

object zio_failure {

  /**
   * EXERCISE 1
   *
   * Using `ZIO.fail` method, create an `IO[String, Int]` value that
   * represents a failure with a string error message, containing
   * a user-readable description of the failure.
   */
  val stringFailure: IO[String, Int] = ZIO.fail("Operation failed")

  /**
   * EXERCISE 2
   *
   * Translate the following exception-throwing program into its ZIO equivalent.
   */
  def accessArr1[A](i: Int, a: Array[A]): A =
    if (i < 0 || i >= a.length)
      throw new IndexOutOfBoundsException(s"The index $i is out of bounds [0, ${a.length} )")
    else a(i)

  def accessArr2[A](i: Int, a: Array[A]): IO[IndexOutOfBoundsException, A] =
    if (i < 0 || i >= a.length)
      ZIO.fail(new IndexOutOfBoundsException(s"The index $i is out of bounds [0, ${a.length} )"))
    else ZIO.succeed(a(i))



  /**
   * EXERCISE 3
   *
   * Using `ZIO#fold`, recover from a division by zero error by supplying a recovery
   * value.
   */
  def divide(n: Int, d: Int): IO[ArithmeticException, Int] =
    if (d == 0) IO.fail(new ArithmeticException("Cannot divide by 0")) else IO.succeed(n / d)
  val recovered1: UIO[Option[Int]] = divide(100, 0).fold(_ => None, result => Some(result))

  /**
   * EXERCISE 4
   *
   * Using `ZIO#foldM`, print out either an error message or the division.
   */
  def printError(err: String): UIO[Unit] = UIO(println(err))
  def printDivision(int: Int): UIO[Unit] = UIO(println("Division is: " + int))
  val recovered2: UIO[Unit]              = printDivision(100).foldM(
                                             _ => printError("Could not divide"),
                                             data => ZIO.succeed(data)
                                           )

  /**
   * EXERCISE 5
   *
   * Using `ZIO#either`, recover from division by zero error by returning -1.
   */
  val recovered3: UIO[Int] = divide(100, 0).either.map {
    case Right(x) => x
    case _        => -1
  }

  /**
   * EXERCISE 6
   *
   * Using `ZIO#option`, recover from division by zero by returning -1.
   */
  val recovered4: UIO[Int] = divide(100, 0).option.map {
    case Some(x) => x
    case _       => -1
  }

  /**
   * EXERCISE 7
   *
   * Using `ZIO#orElse`, attempt `firstChoice`, and fallback to `secondChoice` only
   * if `firstChoice` fails.
   */
  val firstChoice: IO[ArithmeticException, Int] = divide(100, 0)
  val secondChoice: UIO[Int]                    = IO.succeed(-1)
  val combined: UIO[Int]                        = firstChoice.orElse(secondChoice)

  /**
   * EXERCISE 8
   *
   * Using `ZIO#catchAll`, recover from an error.
   */
  val caughtAll: UIO[Int] = divide(100, 0).catchAll(_ => secondChoice)

  /**
   * EXERCISE 9
   *
   * Using `ZIO#catchSome`, recover from only `EmptyStringError` error.
   */
  case object EmptyStringError extends Throwable
  val readNumber: Task[Int] = UIO(scala.io.StdIn.readLine()).flatMap { input =>
    if (input == "") IO.fail(EmptyStringError)
    else IO.effect(input.toInt)
  }
  val caughtSome = readNumber.catchSome {
    case EmptyStringError => secondChoice
  }

  /**
   * EXERCISE 10
   *
   * Using `IO.effectTotal`, import code that is really not total.
   */
  val defect1: UIO[Int] = ZIO.effectTotal("this is a short text".charAt(30))

  /**
   * EXERCISE 11
   *
   * Using `ZIO#sandbox`, recover from the defect `defect1`.
   *
   */
  val caught1: UIO[Int] = defect1.sandbox.catchAll {
    case Cause.Die(_: StringIndexOutOfBoundsException) => ZIO.succeed(-1)
    case Cause.Fail(_) => ZIO.succeed(-1)
    case cause => ZIO.halt(cause)
  }

  /**
   * EXERCISE 12
   *
   * Using the `ZIO#catchAll` method, convert any exceptions in reading the
   * specified file into an empty list.
   */
  def readFile(file: File): UIO[List[String]] =
    Task(Source.fromFile(file).getLines.toList).catchAll {
      case _: IOException => IO.succeed(Nil)
    }
}

object impure_to_pure {

  /**
   * EXERCISE 1
   *
   * Translate the following procedural program into ZIO.
   */
  def getName1(print: String => Unit, read: () => String): Option[String] = {
    print("Do you want to enter your name?")
    read().toLowerCase.take(1) match {
      case "y" => Some(read())
      case _ => None
    }
  }

  def getName2[E](print: String => IO[E, Unit], read: IO[E, String]): IO[E, Option[String]] =
    for {
      answer <- print("Do you want to enter your name?") *> read
      name <- if (answer == "y" ) read.map(Some(_)) else UIO(None)
    } yield name

  /**
   * EXERCISE 2
   *
   * Translate the following procedural program into ZIO.
   */
  def ageExplainer1(): Unit = {
    println("What is your age?")
    scala.util.Try(scala.io.StdIn.readLine().toInt).toOption match {
      case Some(age) =>
        if (age < 12) println("You are a kid")
        else if (age < 20) println("You are a teenager")
        else if (age < 30) println("You are a grownup")
        else if (age < 50) println("You are an adult")
        else if (age < 80) println("You are a mature adult")
        else if (age < 100) println("You are elderly")
        else println("You are probably lying.")
      case None =>
        println("That's not an age, try again")

        ageExplainer1()
    }
  }

  import zio.console.{putStrLn, getStrLn}

  def ageExplainer2: UIO[Unit] = {
    for {
      age <- zio.console.putStrLn("What is your age?") *> zio.console.getStrLn
      _ <- scala.util.Try(age.toInt).toOption match {
        case Some(age) =>
          if (age < 12) putStrLn("You are a kid")
          else if (age < 20) putStrLn("You are a teenager")
          else if (age < 30) putStrLn("You are a grownup")
          else if (age < 50) putStrLn("You are an adult")
          else if (age < 80) putStrLn("You are a mature adult")
          else if (age < 100) putStrLn("You are elderly")
          else putStrLn("You are probably lying.")
        case None => putStrLn("That's not an age, try again")
      }
    } yield ()
    ZIO.unit
  }

  /**
   * EXERCISE 3
   *
   * Translate the following procedural program into ZIO.
   */
  def decode1(read: () => Byte): Either[Byte, Int] = {
    val b = read()
    if (b < 0) Left(b)
    else {
      Right(
        b.toInt +
          (read().toInt << 8) +
          (read().toInt << 16) +
          (read().toInt << 24)
      )
    }
  }

  def decode2[E](read: IO[E, Byte]): IO[E, Either[Byte, Int]] = {
    for {
      b0 <- read
      sum <-
        if (b0 < 0)
          ZIO.succeed(Left(b0))
        else
          for {
            b1 <- read.map(_.toInt << 8)
            b2 <- read.map(_.toInt << 16)
            b3 <- read.map(_.toInt << 24)
          } yield Right(b0 + b1 + b2 + b3)
    } yield sum
  }
}

object zio_interop extends DefaultRuntime {

  import scala.concurrent.ExecutionContext.global
  import scala.concurrent.Future

  /**
   * EXERCISE 1
   *
   * Using `Fiber#toFuture`, convert the following `Fiber` into a `Future`.
   */
  val fiber: Fiber[Throwable, Int] = Fiber.succeed(1)
  val fToFuture: UIO[Future[Int]]  = fiber.toFuture

  /**
   * EXERCISE 2
   *
   * Using `Fiber.fromFuture`, convert the following `Future` into a `Fiber`.
   */
  lazy val future1                     = Future(Thread.sleep(1000))(global)
  val fToFiber: Fiber[Throwable, Unit] = Fiber.fromFuture(future1)

  /**
   * EXERCISE 3
   *
   * Using `Task#toFuture`, unsafely convert the following `Task` into `Future`.
   */
  val task1: Task[Int]       = IO.effect("wrong".toInt)

  val tToFuture: Future[Int] = unsafeRun(task1.toFuture)

  /**
   * EXERCISE 4
   *
   * Use `Task.fromFuture` to convert the following Scala `Future` into a
   * ZIO `Task`.
   */
  lazy val future2        = Future.successful("Hello World")
  val task2: Task[String] = Task.fromFuture(_ => future2)

  /**
   * EXERCISE 5
   *
   * Use `Task.fromTry` to convert the `Try` into a ZIO `Task`.
   */
  val tryValue  = scala.util.Failure(new Throwable("Uh oh"))
  val tryEffect = ZIO.fromTry(tryValue)

  /**
   * EXERCISE 6
   *
   * Use `IO.fromOption` to convert the `Option` into a ZIO `IO`.
   */
  val optionValue  = Some("foo")
  val optionEffect = ZIO.fromOption(optionValue)

  /**
   * EXERCISE 7
   *
   * Use `IO.fromEither` to convert the `Either` into a ZIO `IO`.
   */
  val eitherValue  = Right("foo")
  val eitherEffect = ZIO.fromEither(eitherValue)
}

/**
 * ZIO's version of try / finally, try-with-resources.
 */
object zio_resources {
  import java.io.{ File, FileInputStream }
  class InputStream private (is: FileInputStream) {
    def read: IO[Exception, Option[Byte]] =
      IO.effectTotal(is.read).map(i => if (i < 0) None else Some(i.toByte))
    def close: IO[Exception, Unit] =
      IO.effectTotal(is.close())
  }
  object InputStream {
    def openFile(file: File): IO[Exception, InputStream] =
      IO.effectTotal(new InputStream(new FileInputStream(file)))
  }

  /**
   * This following program is the classic paradigm for resource handling using try / finally
   */
  object classic {
    trait Handle

    def openFile(file: String): Handle        = ???
    def closeFile(handle: Handle): Unit       = ???
    def readFile(handle: Handle): Array[Byte] = ???

    // Classic paradigm for safe resource handling using
    // try / finally:
    def safeResource(file: String): Unit = {
      var handle: Handle = null.asInstanceOf[Handle]

      try {
        handle = openFile(file)

        readFile(handle)
      } finally if (handle != null) closeFile(handle)
    }

    def finallyPuzzler(): Unit =
      try {
        try throw new Error("e1")
        finally throw new Error("e2")
      } catch {
        case e: Error => println(e)
      }
  }

  /**
   * EXERCISE 1
   *
   * Rewrite the following procedural program to ZIO, using `IO.fail` and the
   * `ensuring` method.
   */
  var i = 0
  def noChange1(): Unit =
    try {
      i += 1
      throw new Exception("Boom!")
    } finally i -= 1

  val noChange2: Task[Unit] = {
    i += 1
    val finalizer = UIO.effectTotal(i -= 1)
    ZIO.fail(new Exception("Boom!")).ensuring(finalizer)
  }

  /**
   * EXERCISE 2
   *
   * Rewrite the following procedural program to ZIO, using `IO.fail` and the
   * `ensuring` method of the `IO` object.
   */
  def tryCatch1(): Unit =
    try throw new Exception("Uh oh")
    finally println("On the way out...")
  val tryCatch2: Task[Unit] = {
    val finalizer = UIO.effectTotal(println("On the way out..."))
    IO.fail(new Exception("Uh oh")).ensuring(finalizer)
  }

  /**
   * EXERCISE 3
   *
   * Rewrite the `readFile1` function to use `bracket` so resources can be
   * safely cleaned up in the event of errors, defects, or interruption.
   */
  def readFile1(file: File): IO[Exception, List[Byte]] = {
    def readAll(is: InputStream, acc: List[Byte]): IO[Exception, List[Byte]] =
      is.read.flatMap {
        case None       => IO.succeed(acc.reverse)
        case Some(byte) => readAll(is, byte :: acc)
      }

    for {
      stream <- InputStream.openFile(file)
      bytes  <- readAll(stream, Nil)
      _      <- stream.close
    } yield bytes
  }

  def readFile2(file: File): IO[Exception, List[Byte]] = {
    def readAll(is: InputStream, acc: List[Byte]): IO[Exception, List[Byte]] =
      is.read.flatMap {
        case None       => IO.succeed(acc.reverse)
        case Some(byte) => readAll(is, byte :: acc)
      }

    InputStream.openFile(file).bracket(_.close orElse Task.unit) { stream =>
      readAll(stream, Nil)
    }
  }

  /**
   * EXERCISE 4
   *
   * Implement the `tryCatchFinally` method using `bracket` or `ensuring`.
   */
  def tryCatchFinally[E, A](try0: IO[E, A])(catch0: PartialFunction[E, IO[E, A]])(finally0: UIO[Unit]): IO[E, A] =
    try0.catchSome(catch0).ensuring(finally0)

  /**
   * EXERCISE 5
   *
   * Use the `bracket` method to rewrite the following snippet to ZIO.
   */
  def readFileTCF1(file: File): List[Byte] = {
    var fis: FileInputStream = null

    try {
      fis = new FileInputStream(file)
      val array = Array.ofDim[Byte](file.length.toInt)
      fis.read(array)
      array.toList
    } catch {
      case e: java.io.IOException => Nil
    } finally if (fis != null) fis.close()
  }

  def readFileTCF2(file: File): Task[List[Byte]] = {
    IO.effectTotal(new FileInputStream(file)).bracket(x => IO.effectTotal(x.close())) { fis =>
      for {
        array <- IO(Array.ofDim[Byte](file.length.toInt))
        _     <- IO(fis.read(array))
      } yield array.toList
    }
  }

  /**
   *`Managed[E, A]` is a managed resource of type `A`, which may be used by
   * invoking the `use` method of the resource. The resource will be automatically
   * acquired before the resource is used, and automatically released after the
   * resource is used.
   */
  /**
   * EXERCISE 6
   *
   * Using the `Managed.make` constructor, create a `Managed` resource
   * for a `FileInputStream`.
   */
  def managedFile(file: File): Managed[Throwable, FileInputStream] =
    Managed.make(IO.effect(new FileInputStream(file)))(fs => IO.effectTotal(fs.close))


  /**
   * EXERCISE 7
   *
   * Use the `Managed#use` method to consume the `FileInputStream`, reading
   * all content into a byte array (see Exercise 5).
   */
  def readFileTCF3(file: File): Task[List[Byte]] =
    managedFile(file).use { _ =>
      readFileTCF2(file)
    }
}

object zio_environment {
  import zio.console.Console
  import zio.console
  import zio.clock.Clock
  import zio.clock
  import zio.random.Random
  import zio.random

  /**
   * The Default Modules in ZIO:
   *
   * Console   (putStrLn, getStrLn)
   * Clock     (currentTime, sleep, nanoTime)
   * Random    (nextInt, nextBoolean, ...)
   * System    (env)
   * Blocking  (blocking, effectBlocking)
   * Scheduler (scheduledExecutor)
   */
  /**
   * EXERCISE 1
   *
   * Write the type of a program that requires `scalaz.zio.clock.Clock` and which
   * could fail with `E` or succeed with `A`.
   */
  type ClockIO[E, A] = ZIO[Clock, E, A]

  /**
   * EXERCISE 2
   *
   * Write the type of a program that requires `scalaz.zio.console.Console` and
   * which could fail with `E` or succeed with A`:
   */
  type ConsoleIO[E, A] = ZIO[Console, E, A]

  /**
   * EXERCISE 3
   *
   * Write the type of a program that requires `scalaz.zio.system.System` and
   * which could fail with E or succeed with A:
   */
  type SystemIO[E, A] = ZIO[System, E, A]

  /**
   * EXERCISE 4
   *
   * Write the type of a program that requires `scalaz.zio.random.Random` and
   * which could fail with `E` or succeed with `A`:
   */
  type RandomIO[E, A] = ZIO[Random, E, A]

  /**
   * EXERCISE 5
   *
   * Write the type of a program that requires `Clock` and `System` and which
   * could fail with `E` or succeed with `A`:
   */
  type ClockWithSystemIO[E, A] = ZIO[Clock with System, E, A]

  /**
   * EXERCISE 6
   *
   * Write the type of a program that requires `Console` and `System` and
   * which could fail with `E` or succeed with `A`:
   */
  type ConsoleWithSystemIO[E, A] = ZIO[Console with System, E, A]

  /**
   * EXERCISE 7
   *
   * Write the type of a program that requires `Clock`, `System` and `Random`
   * and which could fail with `E` or succeed with `A`:
   */
  type ClockWithSystemWithRandom[E, A] = ZIO[Clock with System with Random, E, A]

  /**
   * EXERCISE 8
   *
   * Write the type of a program that requires `Clock`, `Console`, `System` and
   * `Random` and which could fail with `E` or succeed with `A`:
   */
  type ClockWithConsoleWithSystemWithRandom[E, A] = ZIO[Clock with Console with System with Random, E, A]

  /**
   * EXERCISE 9
   *
   * Using `zio.console.putStrLn`, write a hello world program, and identify
   * the correct ZIO type to use.
   */
  val helloWorld: ZIO[Console, IOException, Unit] = zio.console.putStrLn("Hello World")

  /**
   * EXERCISE 10
   *
   * Using `zio.console.getStrLn` and `zio.console.putStrLn`, create an
   * interactive program and identify the correct ZIO type to use.
   */
  def interactiveProgram: ZIO[Console, IOException, Unit] =
    for {
      name <- zio.console.getStrLn
      _    <- zio.console.putStrLn(s"Hello $name")
    } yield ()

    /**
     * EXERCISE 11
     *
     * In a for comprehension, call various methods in zio.clock._, zio.console._,
     * and zio.random._, and identify the composite return type.
     */
    import random._
    val program: ZIO[Clock with Console with Random, IOException, Int] =
      for {
        _   <- clock.currentDateTime
        _   <- console.getStrLn
        rnd <- random.nextInt
      } yield rnd

  /**
   * Build a new Service called `Configuration`
   * - define the module
   * - define the interface
   * - define the helper functions (host, port)
   * - implement a trait `Live` that extends the module.
   * - implement all helper functions.
   */
  /**
   * EXERCISE 12
   *
   * Build a `Config` module that has a reference to a `Config.Service` trait.
   */
  trait Config {
    val config: Config.Service
  }

  object Config {
    // Service: definition of the methods provided by module:
    trait Service {
      val port: UIO[Int]
      val host: UIO[String]
    }

    /**
     * EXERCISE 13
     *
     * Implement a production version of the `Config` module.
     */
    trait Live extends Config {
      val config: Config.Service = new Service {
        val port: UIO[Int] = UIO.succeed(443)
        val host: UIO[String] = UIO.succeed("216.58.215.238")
      }
    }
    object Live extends Live
  }

  /**
   * EXERCISE 14
   *
   * Using `ZIO.accessM`, implement helpers, which access the `Config` module
   * and delegate to the functions inside the `Config` service.
   */
  object helpers {
    val port: ZIO[Config, Nothing, Int]    = ZIO.accessM[Config](_.config.port)
    val host: ZIO[Config, Nothing, String] = ZIO.accessM[Config](_.config.host)
  }

  /**
   * EXERCISE 15
   *
   * Write a program that depends on `Config` and `Console` and use the Scala
   * compiler to infer the correct type.
   */
  val configProgram: ZIO[Console with Config, Nothing, Int] = {
    for {
      port <- helpers.port
      host <- helpers.host
      _ <- zio.console.putStrLn(s"$port:$host")
    } yield port
  }

  /**
   * EXERCISE 16
   *
   * Give the `configProgram` its dependencies by supplying it with both `Config`
   * and `Console` modules, and determine the type of the resulting effect.
   */
  val provided: ZIO[Any, Nothing, Int] = configProgram.provide(new Console.Live with Config.Live)

  /**
   * EXERCISE 17
   *
   * Create a `Runtime[Config with Console]` that can be used to run any
   * effect that has a dependency on `Config`:
   */
  val ConfigRuntime: Runtime[Config with Console] =
    Runtime(new Config.Live with Console.Live: Config with Console, PlatformLive.Default)

  /**
   * EXERCISE 18
   *
   * Define a ZIO value that describes an effect which uses Config with
   * Console that displays the port and host in the Console and fails
   * with a String if the host name contains `:`
   */
  val simpleConfigProgram: ZIO[Console with Config, String, Unit] =
    for {
      h <- helpers.host
      _ <- helpers.port.flatMap { p =>
        if (h.contains(":")) ZIO.fail("host cannot contain ':'")
        else zio.console.putStrLn(s"$h:$p")
      }
    } yield ()

  /**
   * EXERCISE 19
   *
   * Run the `simpleConfigProgram` using `ConfigRuntime.unsafeRun`.
   */
  val run: Unit = ConfigRuntime.unsafeRun(simpleConfigProgram)

  /**
   * Build a file system service
   */
  /**
   * EXERCISE 20
   *
   * Build a module for a `FileSystem`.
   */
  trait FileSystem {
    val filesystem: FileSystem.Service[Any]
  }

  object FileSystem {

    /**
     * EXERCISE 21
     *
     * Create a service defining the capabilities of a `FileSystem`.
     */
    trait Service[R] {
      def exists(path: String): ZIO[FileSystem, Throwable, Boolean]
      def listDirFiles(dir: File): ZIO[FileSystem, Throwable, Array[File]]
    }

    /**
     * EXERCISE 22
     *
     * Create a production implementation of the `FileSystem` module.
     */
    trait Live extends FileSystem with Console {
      val filesystem: FileSystem.Service[Any] = new Service[Any] {
        def exists(path: String): ZIO[FileSystem, Throwable, Boolean] =
          IO(new File(path).exists)

        def listDirFiles(dir: File): ZIO[FileSystem, Throwable, Array[File]] =
          ZIO(dir.listFiles)
      }
    }

    object Live extends Live with Console.Live
  }

  /**
   * EXERCISE 23
   *
   * Using `ZIO.accessM`, create helpers.
   */
  object fs extends FileSystem.Service[FileSystem] {
    def exists(path: String): ZIO[FileSystem, Throwable, Boolean] =
      ZIO.accessM[FileSystem](_.filesystem.exists(path))

    def listDirFiles(dir: File): ZIO[FileSystem, Throwable, Array[File]] =
      ZIO.accessM[FileSystem](_.filesystem.listDirFiles(dir))
  }

  /**
   * EXERCISE 24
   *
   * Write a simple program that uses `FileSystem with Console`.
   */
  val fileProgram: ZIO[FileSystem with Console, Throwable, Unit] = {
    val path = "/Users/jjtmccabe"
    for {
      doesExist <- fs.exists(path)
      out       <- if (!doesExist) zio.console.putStrLn(s"$path does not exist")
                   else for {
                     files    <- fs.listDirFiles(new File(path)).map(_.mkString("\n"))
                     strFiles <- zio.console.putStrLn(files)
                   } yield strFiles
    } yield out
  }

  /**
   * EXERCISE 25
   *
   * Create a `Runtime` that can execute effects that require
   * `FileSystem with Console`.
   */
  val FSRuntime: Runtime[FileSystem with Console] =
    Runtime(FileSystem.Live, PlatformLive.Default)

  /**
   * EXERCISE 26
   *
   * Execute `fileProgram` using `FSRuntime.unsafeRun`.
   */
  lazy val fileProgramLive: Unit = FSRuntime.unsafeRun(fileProgram)

  /*
   * With regard to the next two exercises:
   * Today mocking would rely on ZLayer and zio-test as described here:
   *
   * https://zio.dev/docs/howto/howto_mock_services
   */

  /**
   * EXERCISE 27
   *
   * Implement a mock file system module.
   */

  trait MockFileSystem extends FileSystem with Console {
    val filesystem: FileSystem.Service[Any] = new Service[Any] {
      def exists(path: String): ZIO[FileSystem, Throwable, Boolean] =
        ZIO.succeed(true)

      def listDirFiles(dir: File): ZIO[FileSystem, IOException, Array[File]] =
        ZIO.succeed(Array())
    }
    val console: Console.Service[Any] = new Console.Service[Any] {
      def putStr(line: String): ZIO[Any, Nothing, Unit] = ZIO.unit
      def putStrLn(line: String): ZIO[Any, Nothing, Unit] = ZIO.unit
      val getStrLn: ZIO[Any, IOException, String] = ZIO.succeed("")
    }
  }

  /**
   * EXERCISE 28
   *
   * Using `ZIO#provide` with the mock file system module, and a default
   * runtime, execute `fileProgram`.
   */
  lazy val fileProgramTest: Unit = new DefaultRuntime {}.unsafeRun {
    fileProgram.provide(new MockFileSystem {}).run
  }
}
