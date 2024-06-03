package kind.logic

import scala.reflect.ClassTag

/** This is to represent the actors in a system. The people and things which send messsages and data
  * to each other
  *
  * @param type
  *   what kind of thing is this?
  * @param category
  *   what group does this belong to? (e.g. a suite of services)
  * @param label
  *   what should we call this thing?
  */
case class Actor(`type`: ActorType, category: String, label: String) {
  def withName(newName: String)    = copy(label = newName)
  def withType(newType: ActorType) = copy(`type` = newType)
  def qualified                    = s"$category.$label"
  override def toString            = s"$qualified ${`type`.icon}"
}
object Actor:
  def person(category: String, label: String)     = Actor(ActorType.Person, category, label)
  def database(category: String, label: String)   = Actor(ActorType.Database, category, label)
  def queue(category: String, label: String)      = Actor(ActorType.Queue, category, label)
  def job(category: String, label: String)        = Actor(ActorType.Job, category, label)
  def email(category: String, label: String)      = Actor(ActorType.Email, category, label)
  def service(category: String, label: String)    = Actor(ActorType.Service, category, label)
  def fileSystem(category: String, label: String) = Actor(ActorType.FileSystem, category, label)

  def service[A: ClassTag]: Actor = forClass(summon[ClassTag[A]].runtimeClass).service
  def job[A: ClassTag]: Actor     = forClass(summon[ClassTag[A]].runtimeClass).job

  final class Builder(category: String, label: String) {
    def person     = Actor.person(category, label)
    def database   = Actor.database(category, label)
    def queue      = Actor.queue(category, label)
    def job        = Actor.job(category, label)
    def email      = Actor.email(category, label)
    def service    = Actor.service(category, label)
    def fileSystem = Actor.fileSystem(category, label)
  }

  def apply(category: String, label: String): Builder = new Builder(category, label)

  def apply[A: ClassTag](svc: A): Builder = apply[A]

  def apply[A: ClassTag]: Builder = forClass(summon[ClassTag[A]].runtimeClass)

  def forClass(c1ass: Class[?]): Builder = {
    val fullyQualifiedName = c1ass.getName
    val parts              = fullyQualifiedName.split("\\.", -1).toSeq
    val packageName        = parts.init.last
    val name               = parts.last.takeWhile(_ != '$')

    new Builder(packageName, name)
  }
