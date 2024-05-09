package kind.logic

/** The type of 'actor' (or participant) in our system
  */
enum ActorType:
  case Person, Database, Queue, Email, Service, Job, FileSystem

  def icon = this match
    case Person     => "👤"
    case Database   => "🗄️"
    case Queue      => "📤"
    case Email      => "📧"
    case Service    => "🖥️"
    case Job        => "🤖"
    case FileSystem => "📁"
