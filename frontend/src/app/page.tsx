import Image from "next/image";
import Link from 'next/link';
import RegistrationModal from './components/RegistrationModal';

export default function HomePage({ searchParams }: { searchParams: { registered?: string } }){
  const announcements = [
    {
      id: 1,
      title: "Harmonogram lotów - Sezon 2026",
      date: "28-07-2026",
      content: "\n" +
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam non fringilla quam. Donec sem mi, facilisis sed est vitae, mollis auctor magna. " +
          "Etiam pharetra at augue dapibus blandit. Donec ornare vulputate sapien, vel rhoncus lectus pretium at. Cras dictum ante lectus, sit amet pharetra nisl aliquam id. " +
          "Fusce vestibulum eu lacus sit amet aliquet. Quisque ipsum dolor, sodales vitae ultrices et, rutrum id leo. Sed sollicitudin, quam at sodales sodales, " +
          "justo mauris suscipit nisl, id scelerisque purus enim eget mauris. Ut vehicula felis ac odio molestie, vitae feugiat lacus finibus. In ornare velit at mauris ultricies, " +
          "id iaculis tortor malesuada. Cras ligula quam, bibendum nec neque eget, faucibus condimentum massa. Duis luctus cursus diam id viverra. " +
          "Nulla tempus diam quis eros placerat pulvinar. In varius lacus vehicula, placerat libero in, mollis lorem. Proin porttitor nibh ac fringilla fermentum. " +
          "Duis convallis leo non lorem malesuada viverra.",
      author: "Jan Kowalski"
    },
    {
      id: 2,
      title: "Zebranie sekcji",
      date: "25-07-2026",
      content: "\n" +
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam non fringilla quam. Donec sem mi, facilisis sed est vitae, mollis auctor magna. " +
          "Etiam pharetra at augue dapibus blandit. Donec ornare vulputate sapien, vel rhoncus lectus pretium at. Cras dictum ante lectus, sit amet pharetra nisl aliquam id. " +
          "Fusce vestibulum eu lacus sit amet aliquet. Quisque ipsum dolor, sodales vitae ultrices et, rutrum id leo. Sed sollicitudin, quam at sodales sodales, " +
          "justo mauris suscipit nisl, id scelerisque purus enim eget mauris. Ut vehicula felis ac odio molestie, vitae feugiat lacus finibus. In ornare velit at mauris ultricies, " +
          "id iaculis tortor malesuada. Cras ligula quam, bibendum nec neque eget, faucibus condimentum massa. Duis luctus cursus diam id viverra. " +
          "Nulla tempus diam quis eros placerat pulvinar. In varius lacus vehicula, placerat libero in, mollis lorem. Proin porttitor nibh ac fringilla fermentum. " +
          "Duis convallis leo non lorem malesuada viverra." +
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce varius neque et diam rutrum, ut dapibus felis facilisis. Proin tincidunt velit sed elit " +
          "placerat vulputate. Mauris viverra a ante ut volutpat. Proin nec arcu id nulla laoreet fermentum eget non massa. Cras tristique metus felis, vitae bibendum " +
          "dolor feugiat at. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nullam elementum felis sit amet mi euismod sodales. " +
          "Vestibulum non elementum eros, ac vehicula sapien. Vivamus pretium vestibulum lacus, ut laoreet erat consequat vel. Nulla facilisi. Mauris sodales fringilla velit " +
          "vitae mattis.",
      author: "Prezes Sekcji"
    }
  ];

  return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <RegistrationModal />
        <nav className="bg-blue-700 text-white shadow-md">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16 items-center">
              <div className="flex-shrink-0 font-bold text-xl tracking-wider">
                PZHGP Żagań
              </div>
              <div className="flex space-x-4">
                <Link href="/login" className="hover:bg-blue-600 px-3 py-2 rounded-md text-sm font-medium transition">
                  Zaloguj się
                </Link>
                <Link href="/register" className="bg-white text-blue-700 hover:bg-gray-100 px-3 py-2 rounded-md text-sm font-bold transition shadow-sm">
                  Rejestracja
                </Link>
              </div>
            </div>
          </div>
        </nav>

        <header className="bg-white shadow">
          <div className="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:px-8 text-center">
            <h1 className="text-4xl font-extrabold text-gray-900 sm:text-5xl sm:tracking-tight lg:text-6xl">
              Oddział 0368 Żagań
            </h1>
            <p className="max-w-xl mt-5 mx-auto text-xl text-gray-500">
              Oficjalny portal Polskiego Związku Hodowców Gołębi Pocztowych. Śledź wyniki, analizuj plany lotów i bądź na bieżąco z życiem oddziału.
            </p>
            <div className="mt-8 flex justify-center gap-4">
              <Link href="/results" className="bg-blue-600 border border-transparent rounded-md shadow-sm py-3 px-8 text-base font-medium text-white hover:bg-blue-700 transition">
                Wyniki lotów
              </Link>
              <Link href="/contact" className="bg-white border border-gray-300 rounded-md shadow-sm py-3 px-8 text-base font-medium text-gray-700 hover:bg-gray-50 transition">
                Kontakt z zarządem
              </Link>
            </div>
          </div>
        </header>

        <main className="flex-grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8 grid grid-cols-1 lg:grid-cols-3 gap-8">

          <section className="lg:col-span-2 space-y-6">
            <div className="flex items-center justify-between border-b pb-2">
              <h2 className="text-2xl font-bold text-gray-800">Najnowsze ogłoszenia</h2>
            </div>

            <div className="space-y-6">
              {announcements.map((post) => (
                  <article key={post.id} className="bg-white p-6 rounded-lg shadow-sm border border-gray-100 hover:shadow-md transition duration-200">
                    <div className="flex justify-between items-center mb-2">
                      <h3 className="text-xl font-bold text-blue-700">{post.title}</h3>
                      <span className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">{post.date}</span>
                    </div>
                    <p className="text-gray-600 mt-3 leading-relaxed">
                      {post.content}
                    </p>
                    <div className="mt-4 text-sm text-gray-400 font-medium">
                      Dodał: {post.author}
                    </div>
                  </article>
              ))}
            </div>
          </section>

          <aside className="space-y-6">
            <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
              <h3 className="text-lg font-bold text-gray-800 border-b pb-2 mb-4">Szybki dostęp</h3>
              <ul className="space-y-3">
                <li>
                  <Link href="/flight-plan" className="flex items-center text-gray-600 hover:text-blue-600 group transition">
                    <span className="w-2 h-2 bg-blue-500 rounded-full mr-3 group-hover:scale-150 transition-transform"></span>
                    Plan lotów
                  </Link>
                </li>
                <li>
                  <Link href="/download" className="flex items-center text-gray-600 hover:text-blue-600 group transition">
                    <span className="w-2 h-2 bg-blue-500 rounded-full mr-3 group-hover:scale-150 transition-transform"></span>
                    Dokumenty do pobrania
                  </Link>
                </li>
                <li>
                  <Link href="/sections" className="flex items-center text-gray-600 hover:text-blue-600 group transition">
                    <span className="w-2 h-2 bg-blue-500 rounded-full mr-3 group-hover:scale-150 transition-transform"></span>
                    Wykaz sekcji
                  </Link>
                </li>
              </ul>
            </div>

            <div className="bg-gradient-to-br from-blue-500 to-blue-700 p-6 rounded-lg shadow-sm text-white">
              <h3 className="text-lg font-bold mb-2">Warunki lotowe</h3>
              <p className="text-blue-100 text-sm mb-4">Integracja z radarem pogodowym wkrótce...</p>
              <div className="animate-pulse bg-blue-400 h-24 rounded-md"></div>
            </div>
          </aside>
        </main>

        <footer className="bg-gray-800 text-gray-300 py-8 text-center text-sm mt-auto">
          <p>&copy; 2026 Karol Kondracki. Wszelkie prawa zastrzeżone.</p>
          <p className="mt-2 text-gray-500">System realizowany w ramach pracy dyplomowej.</p>
        </footer>
      </div>
  );
}
