package kind.logic

/** The type of 'actor' (or participant) in our system
  */
enum ContainerType:
  case Person, Database, Queue, Email, Service, Job, FileSystem, Script, MobileApp, WebApp,
    DesktopApp, Function

  def icon = this match
    case Person     => "👤"
    case Database   => "🗄️"
    case Queue      => "📤"
    case Email      => "📧"
    case Service    => "🖥️"
    case Job        => "🤖"
    case FileSystem => "📁"
    case Script     => "📜"
    case MobileApp  => "📱"
    case WebApp     => "🌐"
    case DesktopApp => "🖥️"
    case Function   => "λ"
