#include <QApplication>
#include <QWidget>
#include <QPushButton>
#include <QLabel>
#include <QVBoxLayout>

int main(int argc, char *argv[])
{
    QApplication app(argc, argv);

    QWidget window;
    window.setWindowTitle("Qt Wayland Button Test");

    QLabel *label = new QLabel("Count: 0");
    label->setAlignment(Qt::AlignCenter);
    label->setStyleSheet("font-size: 32px;");

    QPushButton *button = new QPushButton("Tap Me");
    button->setStyleSheet("font-size: 28px; padding: 20px;");

    int *count = new int(0);

    QObject::connect(button, &QPushButton::clicked, [&]() {
        (*count)++;
        label->setText(QString("Count: %1").arg(*count));
    });

    QVBoxLayout *layout = new QVBoxLayout;
    layout->addWidget(label);
    layout->addWidget(button);

    window.setLayout(layout);
    window.showFullScreen();   // CRITICAL for Wayland

    return app.exec();
}

