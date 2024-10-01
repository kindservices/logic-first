package kind.logic

import scala.reflect.ClassTag
import upickle.default.*

/** This is to represent the actors in a system. The people and things which send messsages and data
  * to each other
  *
  * @param type
  *   what kind of thing is this?
  * @param softwareSystem
  *   what group does this belong to? (e.g. a suite of services)
  * @param label
  *   what should we call this thing?
  */
case class Container(`type`: ContainerType, softwareSystem: String, label: String)
    derives ReadWriter {
  def withName(newName: String)        = copy(label = newName)
  def withType(newType: ContainerType) = copy(`type` = newType)
  def qualified                        = s"$softwareSystem.$label"
  override def toString                = s"$qualified ${`type`.icon}"
}
object Container:
  def person(softwareSystem: String, label: String) =
    Container(ContainerType.Person, softwareSystem, label)
  def person(using obj: sourcecode.Enclosing) = of(ContainerType.Person)

  def database(softwareSystem: String, label: String) =
    Container(ContainerType.Database, softwareSystem, label)
  def database(using obj: sourcecode.Enclosing) = of(ContainerType.Database)

  def queue(softwareSystem: String, label: String) =
    Container(ContainerType.Queue, softwareSystem, label)
  def queue(using obj: sourcecode.Enclosing) = of(ContainerType.Queue)

  def job(softwareSystem: String, label: String) =
    Container(ContainerType.Job, softwareSystem, label)
  def job(using obj: sourcecode.Enclosing) = of(ContainerType.Job)
  def jobForClass[A: ClassTag]: Container  = forClass(summon[ClassTag[A]].runtimeClass).job

  def script(softwareSystem: String, label: String) =
    Container(ContainerType.Script, softwareSystem, label)
  def script(using obj: sourcecode.Enclosing) = of(ContainerType.Script)

  def webApp(softwareSystem: String, label: String) =
    Container(ContainerType.WebApp, softwareSystem, label)
  def webApp(using obj: sourcecode.Enclosing) = of(ContainerType.WebApp)

  def mobileApp(softwareSystem: String, label: String) =
    Container(ContainerType.MobileApp, softwareSystem, label)
  def mobileApp(using obj: sourcecode.Enclosing) = of(ContainerType.MobileApp)

  def function(softwareSystem: String, label: String) =
    Container(ContainerType.Function, softwareSystem, label)
  def function(using obj: sourcecode.Enclosing) = of(ContainerType.Function)

  def email(softwareSystem: String, label: String) =
    Container(ContainerType.Email, softwareSystem, label)
  def email(using obj: sourcecode.Enclosing) = of(ContainerType.Email)

  def fileSystem(softwareSystem: String, label: String) =
    Container(ContainerType.FileSystem, softwareSystem, label)
  def fileSystem(using obj: sourcecode.Enclosing) = of(ContainerType.FileSystem)

  def service(softwareSystem: String, label: String) =
    Container(ContainerType.Service, softwareSystem, label)
  def service(using obj: sourcecode.Enclosing) = of(ContainerType.Service)
  def serviceForClass[A: ClassTag]: Container  = forClass(summon[ClassTag[A]].runtimeClass).service

  /** of is a factory method, allowing call-sites to use a DSL syntax
    * @param typ
    *   the container type
    * @param enclosingScope
    *   the enclosing scope
    * @return
    *   the Container for the given type, using the enclosing scope as the system and container
    *   names
    */
  def of(typ: ContainerType)(using enclosingScope: sourcecode.Enclosing) = {
    // to get a C4 diagram, we'll need to extract (1) the 'Software System' and (2) the container (app, data store, etc)
    val (system, name) = systemAndContainer

    // enclosing will be something like 'kind.examples.simple.App#assets.Onboarding.System'
    // to get a C4 diagram, we'll need to extract (1) the 'Software System' and (2) the container (app, data store, etc)
    Container(typ, system, name)
  }

  final class Builder(softwareSystem: String, label: String) {
    def person                    = Container.person(softwareSystem, label)
    def database                  = Container.database(softwareSystem, label)
    def queue                     = Container.queue(softwareSystem, label)
    def job                       = Container.job(softwareSystem, label)
    def email                     = Container.email(softwareSystem, label)
    def service                   = Container.service(softwareSystem, label)
    def fileSystem                = Container.fileSystem(softwareSystem, label)
    def build(typ: ContainerType) = Container(typ, softwareSystem, label)
  }

  def apply(softwareSystem: String, label: String): Builder = new Builder(softwareSystem, label)

  def apply[A: ClassTag](svc: A): Builder = apply[A]

  def apply[A: ClassTag]: Builder = forClass(summon[ClassTag[A]].runtimeClass)

  def forClass(c1ass: Class[?]): Builder = {
    val fullyQualifiedName = c1ass.getName
    val parts              = fullyQualifiedName.split("\\.", -1).toSeq
    val packageName        = parts.init.last
    val name               = parts.last.takeWhile(_ != '$')

    new Builder(packageName, name)
  }

  /** convenience method to return the system and container from the code scope
    *
    * @param enclosingScope
    *   the enclosing scope
    * @return
    *   the parsed 'system' (context) and container from the enclosing code scope
    */
  def systemAndContainer(using enclosingScope: sourcecode.Enclosing): (String, String) = {
    def splitOn(text : String, chars : Seq[String]) : Seq[String] = chars match {
        case Seq() => Seq(text)
        case head +: tail => text.split(head).toSeq.flatMap(w => splitOn(w, tail))
      }

    splitOn(enclosingScope.value, Seq("#", "\\.", " ")) match {
      case Seq(only) => ("", only)
      case many => (many.init.last, many.last)
    }
  }
