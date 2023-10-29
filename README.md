# Ark: Survival Ascended - Server startup tester

**Warning! Only to help the tech savvy. I'm using this for my own server, and decided to publish it to help others. But I don't
have the time to make it user friendly / deliver support on it. If you're not a developer, you probably won't be able
to use this. I will not be held responsible for any damage caused by this script. Use at your own risk.**

## What is this?

A common problem for Ark: SA is the server ending up in a crash loop. By loading up your save in Single Player Non-Dedicated,
you will probably get a pop-up with a crash report (stacktrace). If that stacktrace includes loading tribe files, 
then there's a big chance one of your tribe files is corrupted.

This script tries to find the tribe files that are corrupt by repeatedly loading the server with a specific set of 
tribe files, and narrowing down to the corrupt file(s), using binary search. This means you'll need between `log(n)` (best case)
and `n` server restarts to find the corrupt tribe files, where n is the number of tribes.

## Usage

1. Backup any save files, as this script will delete the destination folder with every try.
2. Put your broken files in the `save_files` directory.
3. Download the Ark: Survival Ascended dedicated server package from Steam.
4. Modify Main.java with the correct paths to your dedicated server installation.
5. Clear `bad_tribes.txt` and `good_tribes.txt` if needed (this is used to persist and continue a single test run)
6. Run the script. It will start the server with all tribes, and then narrow down to the corrupt ones. It will print the corrupt tribes in the end, as well as output them in  bad_tribes.txt` file.
7. Remove the arktribe files that are reported as bad.